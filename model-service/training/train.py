"""APTOS 2019 训练入口。

用法示例（从 model-service 目录执行）：
    python -m training.train --data-root ../datasets/aptos2019_224x224 --epochs 30 --batch-size 32 --lr 3e-4

说明：
- 默认使用 ImageNet 预训练初始化（首次会下载权重，需联网 / 开代理）；
  离线训练加 --no-pretrained 走随机初始化。
- 骨干与推理服务 model_loader 一致（MobileNetV3-Small，5 类头）；
  最佳权重直接导出到 models/best_model.pth，推理服务零代码切换。
- 类别不均衡通过加权 CrossEntropyLoss 缓解；按验证集宏平均 F1 选优。
"""
from __future__ import annotations

import argparse
import json
import logging
import random
from pathlib import Path

import numpy as np
import torch
import torch.nn as nn
from torch.optim import AdamW
from torch.optim.lr_scheduler import CosineAnnealingLR
from torchvision import models

from app.config import NUM_CLASSES, BACKBONE, MODEL_WEIGHTS_PATH, DEVICE
from .dataset import build_loaders

logger = logging.getLogger("drs.training.train")


def set_seed(seed: int = 42) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)


def build_model(
    pretrained: bool = True,
    num_classes: int = NUM_CLASSES,
    backbone: str = BACKBONE,
) -> nn.Module:
    """构建骨干并替换分类头，结构与推理服务 model_loader.build_backbone 一致。"""
    if backbone == "mobilenet_v3_small":
        model = models.mobilenet_v3_small(
            weights=models.MobileNet_V3_Small_Weights.IMAGENET1K_V1 if pretrained else None
        )
        head = model.classifier[-1]
        model.classifier[-1] = nn.Linear(head.in_features, num_classes)
    elif backbone == "efficientnet_b0":
        model = models.efficientnet_b0(
            weights=models.EfficientNet_B0_Weights.IMAGENET1K_V1 if pretrained else None
        )
        head = model.classifier[1]
        model.classifier[1] = nn.Linear(head.in_features, num_classes)
    else:
        raise ValueError(f"不支持的骨干网络: {backbone}")
    return model


def _metrics(preds, targets, num_classes: int):
    preds = np.asarray(preds)
    targets = np.asarray(targets)
    cm = np.zeros((num_classes, num_classes), dtype=np.int64)
    for p, t in zip(preds, targets):
        cm[t, p] += 1
    f1s, counts = [], []
    for c in range(num_classes):
        tp = int(cm[c, c])
        fp = int(cm[:, c].sum()) - tp
        fn = int(cm[c, :].sum()) - tp
        prec = tp / (tp + fp) if (tp + fp) else 0.0
        rec = tp / (tp + fn) if (tp + fn) else 0.0
        f1 = 2 * prec * rec / (prec + rec) if (prec + rec) else 0.0
        f1s.append(f1)
        counts.append(int(cm[c, :].sum()))
    acc = float((preds == targets).mean()) if len(preds) else 0.0
    macro_f1 = float(np.mean(f1s)) if f1s else 0.0
    return acc, macro_f1, f1s, counts


@torch.no_grad()
def evaluate(model: nn.Module, loader, device, num_classes: int = NUM_CLASSES):
    model.eval()
    all_p, all_t = [], []
    for x, y in loader:
        x, y = x.to(device, non_blocking=True), y.to(device, non_blocking=True)
        out = model(x)
        p = out.argmax(1).cpu().numpy()
        all_p.extend(p.tolist())
        all_t.extend(y.cpu().numpy().tolist())
    return _metrics(all_p, all_t, num_classes)


def main() -> None:
    parser = argparse.ArgumentParser(description="APTOS 2019 DR 分级训练")
    parser.add_argument("--data-root", required=True, help="数据集根目录（含 colored_images/ 或 train.csv）")
    parser.add_argument("--epochs", type=int, default=30)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--lr", type=float, default=3e-4)
    parser.add_argument("--weight-decay", type=float, default=1e-4)
    parser.add_argument("--num-workers", type=int, default=4)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--pretrained", dest="pretrained", action="store_true", default=True,
                        help="ImageNet 预训练初始化（默认开启）")
    parser.add_argument("--no-pretrained", dest="pretrained", action="store_false",
                        help="随机初始化（离线训练用）")
    parser.add_argument("--weight-scheme", choices=["inverse", "effective"], default="inverse",
                        help="类别加权方案，缓解不均衡")
    parser.add_argument("--persistent-workers", dest="persistent_workers", action="store_true",
                        default=True,
                        help="复用 DataLoader worker 进程（Windows 下每 epoch 省约 13.7s，默认开启）")
    parser.add_argument("--no-persistent-workers", dest="persistent_workers", action="store_false",
                        help="每个 epoch 重建 worker（排障用，会显著变慢）")
    parser.add_argument("--val-ratio", type=float, default=0.1)
    parser.add_argument("--test-ratio", type=float, default=0.1)
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO,
                        format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    set_seed(args.seed)

    device = torch.device(DEVICE)
    if device.type == "cuda":
        # 输入尺寸固定为 224×224，让 cuDNN 自动挑选最优卷积算法
        torch.backends.cudnn.benchmark = True
    logger.info("设备: %s | 骨干: %s | 预训练: %s | persistent_workers: %s",
                device, BACKBONE, args.pretrained, args.persistent_workers)

    train_loader, val_loader, test_loader, class_weights = build_loaders(
        args.data_root,
        batch_size=args.batch_size,
        num_workers=args.num_workers,
        val_ratio=args.val_ratio,
        test_ratio=args.test_ratio,
        seed=args.seed,
        weight_scheme=args.weight_scheme,
        persistent_workers=args.persistent_workers,
    )

    model = build_model(pretrained=args.pretrained).to(device)
    criterion = nn.CrossEntropyLoss(weight=class_weights.to(device))
    optimizer = AdamW(model.parameters(), lr=args.lr, weight_decay=args.weight_decay)
    scheduler = CosineAnnealingLR(optimizer, T_max=args.epochs)

    best_macro_f1 = 0.0
    history = []
    for epoch in range(1, args.epochs + 1):
        model.train()
        running_loss, correct, total = 0.0, 0, 0
        for x, y in train_loader:
            x, y = x.to(device, non_blocking=True), y.to(device, non_blocking=True)
            optimizer.zero_grad()
            out = model(x)
            loss = criterion(out, y)
            loss.backward()
            optimizer.step()
            running_loss += loss.item() * x.size(0)
            correct += (out.argmax(1) == y).sum().item()
            total += x.size(0)
        scheduler.step()

        train_loss = running_loss / max(total, 1)
        train_acc = correct / max(total, 1)
        val_acc, val_macro_f1, val_f1s, _ = evaluate(model, val_loader, device)

        logger.info(
            "Epoch %d/%d | loss %.4f acc %.4f | val acc %.4f macroF1 %.4f | F1/类 %s",
            epoch, args.epochs, train_loss, train_acc, val_acc, val_macro_f1,
            [round(f, 4) for f in val_f1s],
        )
        history.append({
            "epoch": epoch, "train_loss": train_loss, "train_acc": train_acc,
            "val_acc": val_acc, "val_macro_f1": val_macro_f1, "val_f1_per_class": val_f1s,
        })

        if val_macro_f1 > best_macro_f1:
            best_macro_f1 = val_macro_f1
            MODEL_WEIGHTS_PATH.parent.mkdir(parents=True, exist_ok=True)
            torch.save(model.state_dict(), MODEL_WEIGHTS_PATH)
            logger.info("已保存最佳权重 -> %s (macroF1=%.4f)", MODEL_WEIGHTS_PATH, best_macro_f1)

    # 用最佳权重在测试集上最终评估
    if MODEL_WEIGHTS_PATH.exists():
        sd = torch.load(MODEL_WEIGHTS_PATH, map_location="cpu", weights_only=True)
        model.load_state_dict(sd, strict=False)
    test_acc, test_macro_f1, test_f1s, test_counts = evaluate(model, test_loader, device)
    logger.info("测试集 -> acc %.4f macroF1 %.4f | F1/类 %s | 各类样本数 %s",
                test_acc, test_macro_f1, [round(f, 4) for f in test_f1s], test_counts)

    metrics = {
        "best_val_macro_f1": best_macro_f1,
        "test_acc": test_acc,
        "test_macro_f1": test_macro_f1,
        "test_f1_per_class": test_f1s,
        "test_counts": test_counts,
        "history": history,
        "args": vars(args),
    }
    out_json = MODEL_WEIGHTS_PATH.parent / "train_metrics.json"
    out_json.write_text(json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8")
    logger.info("训练指标已写出 -> %s", out_json)


if __name__ == "__main__":
    main()
