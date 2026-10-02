"""在完整测试集上拟合人工复核阈值（归一化预测熵）。

背景
----
人工复核的判定依据是归一化预测熵 `-Σp·ln p / ln 5`，当前阈值 0.20 由 30 张样本
实测确定，未在完整测试集上标定。本脚本用与训练完全一致的划分（分层抽样 8:1:1、
seed=42）取出测试集，逐张推理并评估候选阈值，给出数据驱动的推荐值。

用法
----
    cd model-service
    python ../tools/fit_review_threshold.py

输出
----
- 控制台：准确率、熵分布、候选阈值的「触发率 / 错分召回 / 误报数」对照表
- 逐条结果：`.workbuddy-ai/tmp/threshold-fit-records.json`（供后续复核）
"""
from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT / "model-service"))

from app.config import LEVEL_CODES  # noqa: E402
from app.model_loader import load_model  # noqa: E402
from training.dataset import (  # noqa: E402
    APTOSDataset,
    collect_samples,
    get_transforms,
    stratified_split,
)

LN_CLASSES = math.log(len(LEVEL_CODES))

#: 候选阈值（归一化熵）
CANDIDATES = [0.05, 0.08, 0.10, 0.12, 0.15, 0.18, 0.20, 0.22, 0.25, 0.30, 0.35, 0.40]

#: 选优约束：触发率（人工复核工作量）上限
MAX_TRIGGER_RATE = 0.25


def normalized_entropy(probs: list[float]) -> float:
    """归一化预测熵：0 = 完全确定，1 = 五类均匀分布。"""
    h = -sum(p * math.log(p) for p in probs if p > 0)
    return max(h, 0.0) / LN_CLASSES


def run_inference(model, device, samples, batch_size: int = 32) -> list[dict]:
    dataset = APTOSDataset(samples, get_transforms(train=False))
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=False, num_workers=0)
    records: list[dict] = []
    with torch.no_grad():
        for x, y in loader:
            x = x.to(device, non_blocking=True)
            probs = F.softmax(model(x), dim=1).cpu()
            preds = probs.argmax(dim=1)
            for i in range(probs.size(0)):
                p = probs[i].tolist()
                pred = int(preds[i])
                records.append(
                    {
                        "truth": int(y[i]),
                        "pred": pred,
                        "confidence": round(p[pred], 6),
                        "uncertainty": round(normalized_entropy(p), 6),
                    }
                )
    return records


def evaluate(records: list[dict], wrong: list[dict]) -> None:
    n = len(records)
    confs = sorted(r["confidence"] for r in records)
    ents = sorted(r["uncertainty"] for r in records)
    print(f"\nconfidence  最小 {confs[0]:.4f} / 中位 {confs[n // 2]:.4f} / 最大 {confs[-1]:.4f}")
    print(f"uncertainty 最小 {ents[0]:.4f} / 中位 {ents[n // 2]:.4f} / 最大 {ents[-1]:.4f}")
    print(f"错分样本 entropy: {sorted(round(r['uncertainty'], 4) for r in wrong)}")

    print("\n=== 候选阈值评估（归一化熵 ≥ 阈值 → 触发人工复核）===")
    header = f"{'阈值':>6}{'触发':>7}{'触发率':>9}{'命中错分':>10}{'错分召回':>10}{'误报':>7}"
    print(header)
    print("-" * len(header))

    best = None
    for t in CANDIDATES:
        trig = [r for r in records if r["uncertainty"] >= t]
        hit = [r for r in trig if r["truth"] != r["pred"]]
        recall = len(hit) / len(wrong) if wrong else 0.0
        rate = len(trig) / n
        print(
            f"{t:>6.2f}{len(trig):>7}{rate:>9.1%}"
            f"{len(hit):>7}/{len(wrong):<3}{recall:>9.1%}{len(trig) - len(hit):>7}"
        )
        if rate <= MAX_TRIGGER_RATE:
            # 先看错分召回，召回相同则触发数更少者优先
            if best is None or recall > best[1] + 1e-9 or (
                abs(recall - best[1]) < 1e-9 and len(trig) < best[2]
            ):
                best = (t, recall, len(trig))

    # 对照：旧规则 top1 置信度 < 0.70
    old_trig = [r for r in records if r["confidence"] < 0.70]
    old_hit = [r for r in old_trig if r["truth"] != r["pred"]]
    print("\n=== 对照：旧规则 top1 置信度 < 0.70 ===")
    if wrong:
        print(
            f"触发 {len(old_trig)}/{n}（{len(old_trig) / n:.1%}），"
            f"覆盖错分 {len(old_hit)}/{len(wrong)}（{len(old_hit) / len(wrong):.1%}）"
        )
    else:
        print(f"触发 {len(old_trig)}/{n}（{len(old_trig) / n:.1%}）")

    if best:
        print(
            f"\n推荐阈值: {best[0]:.2f}"
            f"（错分召回 {best[1]:.1%}，触发 {best[2]}/{n} = {best[2] / n:.1%}，"
            f"约束为触发率 ≤ {MAX_TRIGGER_RATE:.0%}）"
        )


def main() -> None:
    data_root = PROJECT_ROOT / "datasets" / "aptos2019_224x224"
    samples, layout = collect_samples(data_root)
    train_s, val_s, test_s = stratified_split(samples, 0.8, 0.1, 0.1, 42)
    print(f"数据布局: {layout.get('mode')}")
    print(f"划分: train {len(train_s)} / val {len(val_s)} / test {len(test_s)}（seed=42）")

    model, meta = load_model()
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = model.to(device).eval()
    print(f"模型: {meta.get('version')}  设备: {device}  trained={meta.get('trained')}")

    records = run_inference(model, device, test_s)
    n = len(records)
    correct = [r for r in records if r["truth"] == r["pred"]]
    wrong = [r for r in records if r["truth"] != r["pred"]]
    print(f"\n测试集准确率: {len(correct) / n:.4f}（正确 {len(correct)} / 错误 {len(wrong)}）")

    out_dir = PROJECT_ROOT / ".workbuddy-ai" / "tmp"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "threshold-fit-records.json"
    out_file.write_text(json.dumps(records, ensure_ascii=False), encoding="utf-8")
    print(f"逐条结果已保存: {out_file}")

    evaluate(records, wrong)


if __name__ == "__main__":
    main()
