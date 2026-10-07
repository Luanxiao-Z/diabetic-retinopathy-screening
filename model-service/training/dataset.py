"""APTOS 2019 训练数据加载。

支持两种目录布局（自动探测）：
- 文件夹布局（sovitrath 224x224 预处理版，默认选择）：
      <root>/colored_images/{No_DR,Mild,Moderate,Severe,Proliferate_DR}/*.png
      也兼容带数字前缀的写法 {0 - No_DR,1 - Mild,2 - Moderate,3 - Severe,4 - Proliferate_DR}/*.png
- CSV 布局（APTOS 官方原版）：
      <root>/train.csv (id_code,diagnosis) + <root>/train_images/{id_code}.png

标签映射与 B_DR_LEVEL / LEVEL_LABELS 一致：No_DR=0, Mild=1, Moderate=2, Severe=3, PDR=4。

提供：
- APTOSDataset：torch Dataset，读取图片 + 标签（0..4）。
- get_transforms：训练增强 / 评估预处理（与推理服务 preprocess 对齐）。
- stratified_split：按标签分层抽样切分 train/val/test。
- compute_class_weights：缓解类别不均衡（逆频率 / 有效样本数）。
- build_loaders：一键构造 (train_loader, val_loader, test_loader, class_weights)。
"""
from __future__ import annotations

import csv
import logging
import math
import random
from collections import Counter
from pathlib import Path

import numpy as np
import torch
from PIL import Image
from torch.utils.data import DataLoader, Dataset, WeightedRandomSampler
from torchvision import transforms

from app.config import IMG_SIZE, IMAGENET_MEAN, IMAGENET_STD, NUM_CLASSES

logger = logging.getLogger("drs.training.dataset")

# 文件夹布局：数字前缀风格 '0 - No_DR' 的子目录名首词即标签 0..4
_FOLDER_PREFIXES = {str(i): i for i in range(NUM_CLASSES)}
# 文件夹布局：类名风格（sovitrath 224x224 实际命名，归一化后匹配）→ 标签 0..4
# 顺序与 B_DR_LEVEL / LEVEL_LABELS 一致：No_DR=0, Mild=1, Moderate=2, Severe=3, PDR=4
_FOLDER_NAME_TO_LABEL = {
    "no_dr": 0,
    "normal": 0,
    "mild": 1,
    "moderate": 2,
    "severe": 3,
    "proliferate_dr": 4,
    "proliferat_dr": 4,
    "pdr": 4,
}
_IMAGE_EXTS = (".png", ".jpg", ".jpeg", ".bmp", ".tif", ".tiff")
_NORMALIZE = transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD)


def _resolve_folder_label(folder_name: str) -> int | None:
    """从类别子目录名解析整数标签 0..4。

    支持两种命名：
    - 数字前缀风格：'0 - No_DR' / '1 - Mild' ...
    - 纯类名风格（sovitrath 224x224 实际）：'No_DR' / 'Mild' / 'Moderate' / 'Severe' / 'Proliferate_DR'
    """
    raw = folder_name.strip()
    first = raw.split()[0]
    if first in _FOLDER_PREFIXES:
        return _FOLDER_PREFIXES[first]
    norm = raw.lower().replace("-", "_").replace(" ", "_")
    return _FOLDER_NAME_TO_LABEL.get(norm)


def discover_layout(root: str | Path) -> dict:
    """探测数据根目录下实际布局，返回布局描述字典。

    兼容 sovitrath 解压后多出一层父目录的情况（自动向下搜索一层）。
    """
    root = Path(root)
    candidates = [root] + [p for p in root.iterdir() if p.is_dir()]
    for base in candidates:
        colored = base / "colored_images"
        if colored.is_dir():
            return {"mode": "folders", "images_dir": colored}
        csv_file = base / "train.csv"
        if csv_file.is_file():
            for name in ("train_images", "images"):
                img_dir = base / name
                if img_dir.is_dir():
                    return {"mode": "csv", "csv_path": csv_file, "images_dir": img_dir}
            return {"mode": "csv", "csv_path": csv_file, "images_dir": base}
    raise FileNotFoundError(
        f"在 {root} 未找到 colored_images/ 或 train.csv，请确认 sovitrath 数据集已正确解压。"
    )


def _collect_folder_samples(images_dir: Path) -> list[tuple[Path, int]]:
    samples: list[tuple[Path, int]] = []
    unmapped = []
    for sub in sorted(images_dir.iterdir()):
        if not sub.is_dir():
            continue
        label = _resolve_folder_label(sub.name)
        if label is None:
            unmapped.append(sub.name)
            continue
        for f in sub.iterdir():
            if f.suffix.lower() in _IMAGE_EXTS:
                samples.append((f, label))
    if unmapped:
        logger.warning("文件夹布局有 %d 个未识别的子目录（已跳过）：%s", len(unmapped), unmapped)
    if not samples:
        raise RuntimeError(
            f"文件夹布局未收集到任何样本：{images_dir}（未识别任何类别子目录，请检查命名）"
        )
    return samples


def _collect_csv_samples(csv_path: Path, images_dir: Path) -> list[tuple[Path, int]]:
    samples: list[tuple[Path, int]] = []
    missing = 0
    with csv_path.open(newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        for row in reader:
            code = row["id_code"].strip()
            label = int(row["diagnosis"])
            img = images_dir / f"{code}.png"
            if not img.exists():
                alt = None
                for ext in (".jpg", ".jpeg", ".bmp", ".tif", ".tiff"):
                    cand = images_dir / f"{code}{ext}"
                    if cand.exists():
                        alt = cand
                        break
                if alt is None:
                    missing += 1
                    continue
                img = alt
            samples.append((img, label))
    if missing:
        logger.warning("CSV 布局有 %d 张图片缺失，已跳过。", missing)
    if not samples:
        raise RuntimeError(f"CSV 布局未收集到任何样本：{csv_path}")
    return samples


def collect_samples(root: str | Path) -> tuple[list[tuple[Path, int]], dict]:
    """收集全部 (path, label) 样本，返回 (samples, layout)。"""
    layout = discover_layout(root)
    if layout["mode"] == "folders":
        samples = _collect_folder_samples(layout["images_dir"])
    else:
        samples = _collect_csv_samples(layout["csv_path"], layout["images_dir"])
    logger.info("共收集 %d 张样本（布局：%s）", len(samples), layout["mode"])
    return samples, layout


def get_transforms(train: bool = True) -> transforms.Compose:
    """返回 transform。

    训练：Resize(256)+RandomCrop(224)+随机翻转/旋转/颜色抖动（眼底图朝向/光照多变，增强鲁棒性）。
    评估/测试：与推理服务 preprocess 完全一致的 Resize(256)+CenterCrop(224) 预处理。
    """
    if train:
        return transforms.Compose([
            transforms.Resize(256),
            transforms.RandomCrop(IMG_SIZE),
            transforms.RandomHorizontalFlip(),
            transforms.RandomVerticalFlip(),
            transforms.RandomRotation(degrees=20),
            transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.1),
            transforms.ToTensor(),
            _NORMALIZE,
        ])
    return transforms.Compose([
        transforms.Resize(256),
        transforms.CenterCrop(IMG_SIZE),
        transforms.ToTensor(),
        _NORMALIZE,
    ])


class APTOSDataset(Dataset):
    """APTOS 2019 数据集（图片路径 + 整数标签 0..4）。"""

    def __init__(self, samples: list[tuple[Path, int]], transform=None):
        self.samples = samples
        self.transform = transform

    def __len__(self) -> int:
        return len(self.samples)

    def __getitem__(self, index: int):
        path, label = self.samples[index]
        try:
            img = Image.open(path).convert("RGB")
        except Exception as exc:  # 解码失败：回退到首张，保证 batch 不中断
            logger.warning("读取失败 %s: %s，已回退首张。", path, exc)
            img = Image.open(self.samples[0][0]).convert("RGB")
            label = self.samples[0][1]
        if self.transform is not None:
            img = self.transform(img)
        return img, label


def stratified_split(
    samples: list[tuple[Path, int]],
    train_ratio: float = 0.8,
    val_ratio: float = 0.1,
    test_ratio: float = 0.1,
    seed: int = 42,
) -> tuple[list, list, list]:
    """按标签分层抽样，返回 (train, val, test) 三个样本子列表。

    保持各类在三个子集中的原始比例，避免少数类（3/4 级）被切分失衡。
    """
    assert abs(train_ratio + val_ratio + test_ratio - 1.0) < 1e-6
    rng = random.Random(seed)
    by_label: dict[int, list] = {}
    for s in samples:
        by_label.setdefault(s[1], []).append(s)

    train_s, val_s, test_s = [], [], []
    for label, items in by_label.items():
        rng.shuffle(items)
        n = len(items)
        n_train = int(round(n * train_ratio))
        n_val = int(round(n * val_ratio))
        train_s.extend(items[:n_train])
        val_s.extend(items[n_train:n_train + n_val])
        test_s.extend(items[n_train + n_val:])
    rng.shuffle(train_s)
    rng.shuffle(val_s)
    rng.shuffle(test_s)
    return train_s, val_s, test_s


def compute_class_weights(
    labels: list[int],
    num_classes: int = NUM_CLASSES,
    scheme: str = "inverse",
    beta: float = 0.9999,
) -> torch.Tensor:
    """计算类别权重以缓解不均衡。

    scheme:
      - 'none'：全部为 1，即不加权。**配合类别重采样时应选此项**——
        重采样已让各类样本数相等，再叠加逆频率权重会造成双重补偿，
        少数类被过度强调而损害多数类性能。
      - 'inverse'：w_c = N / (K * n_c)（逆频率，sklearn 默认）。
      - 'effective'：有效样本数法 w_c ∝ (1-β)/(1-β^{n_c})，β 接近 1 时更强抑制大类。
    """
    counts = np.bincount(labels, minlength=num_classes).astype(np.float64)
    counts = np.where(counts == 0, 1.0, counts)  # 防除零
    if scheme == "none":
        weights = np.ones(num_classes, dtype=np.float64)
    elif scheme == "inverse":
        weights = counts.sum() / (num_classes * counts)
    elif scheme == "effective":
        effective = (1.0 - beta) / (1.0 - np.power(beta, counts))
        weights = effective.sum() / (num_classes * effective)
    else:
        raise ValueError(f"未知 scheme: {scheme}")
    return torch.tensor(weights, dtype=torch.float32)


def build_sampler(
    samples: list[tuple[Path, int]],
    mode: str = "none",
    num_samples: int | None = None,
) -> WeightedRandomSampler | None:
    """构造类别重采样器（**仅用于训练集**）。

    APTOS 训练集最大/最小类相差 9.4 倍（1444 : 154）。不均衡会同时伤害两个方向：
    多数类主导梯度方向，少数类因样本过少而欠拟合（LEVEL_3 的 F1 长期最低）。

    采样层面的两种均衡方式：

    - ``oversample``：每个类被抽中的总概率相等（样本权重 ∝ 1/n_c），**彻底均衡**。
      每轮样本数不变时，少数类被重复约 3.8 倍、多数类被欠采样至 0.41 倍。
    - ``sqrt``：样本权重 ∝ 1/√n_c，**温和均衡**。少数类重复约 2.1 倍，
      多数类仍占 34%，适合少数类样本极少（重复倍数过高会加剧过拟合）的场景。

    验证集与测试集**一律不重采样**，保持原始分布，否则指标失去可比性。

    :param num_samples: 每轮抽取的样本数，默认与原训练集等长（不改变每轮计算量）
    """
    if mode == "none":
        return None

    counts = Counter(label for _, label in samples)
    if mode == "oversample":
        weights = [1.0 / counts[label] for _, label in samples]
    elif mode == "sqrt":
        weights = [1.0 / math.sqrt(counts[label]) for _, label in samples]
    else:
        raise ValueError(f"未知的重采样模式: {mode}（可选 none / oversample / sqrt）")

    n = num_samples or len(samples)
    logger.info(
        "类别重采样 -> 模式=%s 每轮样本=%d 原始分布=%s",
        mode, n, {k: counts[k] for k in sorted(counts)},
    )
    return WeightedRandomSampler(weights, num_samples=n, replacement=True)


def build_loaders(
    root: str | Path,
    batch_size: int = 32,
    num_workers: int = 4,
    train_ratio: float = 0.8,
    val_ratio: float = 0.1,
    test_ratio: float = 0.1,
    seed: int = 42,
    pin_memory: bool = True,
    weight_scheme: str = "inverse",
    persistent_workers: bool = True,
    prefetch_factor: int = 2,
    resample: str = "none",
) -> tuple[DataLoader, DataLoader, DataLoader, torch.Tensor]:
    """一键构造 (train_loader, val_loader, test_loader, class_weights)。

    Windows 性能说明：spawn 模式下重建 worker 需重新 import torch，实测单次
    开销约 13.7s（4 workers）。train/val 每个 epoch 都会重新迭代，故默认开启
    persistent_workers 复用 worker 进程；test 仅迭代一次，反而 num_workers=0
    更省（省去一次 spawn，单线程解码 367 张约 2s）。

    :param resample: 训练集重采样模式（none / oversample / sqrt），见 :func:`build_sampler`
    """
    all_samples, _ = collect_samples(root)
    train_s, val_s, test_s = stratified_split(
        all_samples, train_ratio, val_ratio, test_ratio, seed
    )
    train_ds = APTOSDataset(train_s, get_transforms(train=True))
    val_ds = APTOSDataset(val_s, get_transforms(train=False))
    test_ds = APTOSDataset(test_s, get_transforms(train=False))

    # num_workers=0 时 persistent_workers / prefetch_factor 无意义且会被 torch 拒绝
    worker_kwargs: dict = (
        {"persistent_workers": persistent_workers, "prefetch_factor": prefetch_factor}
        if num_workers > 0 else {}
    )

    # WeightedRandomSampler 与 shuffle 互斥：指定 sampler 时不能再传 shuffle=True
    sampler = build_sampler(train_s, mode=resample)
    train_loader = DataLoader(
        train_ds, batch_size=batch_size,
        sampler=sampler, shuffle=sampler is None,
        num_workers=num_workers, pin_memory=pin_memory, drop_last=True,
        **worker_kwargs,
    )
    val_loader = DataLoader(
        val_ds, batch_size=batch_size, shuffle=False,
        num_workers=num_workers, pin_memory=pin_memory,
        **worker_kwargs,
    )
    test_loader = DataLoader(
        test_ds, batch_size=batch_size, shuffle=False,
        num_workers=0, pin_memory=pin_memory,
    )
    class_weights = compute_class_weights([l for _, l in train_s], scheme=weight_scheme)
    logger.info("数据划分 -> train:%d val:%d test:%d", len(train_s), len(val_s), len(test_s))
    logger.info("类别权重 -> %s", [round(w, 4) for w in class_weights.tolist()])
    return train_loader, val_loader, test_loader, class_weights
