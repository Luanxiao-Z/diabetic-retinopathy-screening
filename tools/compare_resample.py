"""多随机种子重复实验：对比「不重采样」与「类别重采样」的稳健差异。

动机：LEVEL_3 在测试集仅 20 张，单次实验的 F1 会因 1~2 个样本的预测变化而
大幅波动（实测同一配置在验证集升、测试集降）。单点指标不足以支撑结论，
故在多个随机种子（改变数据划分）下重复，比较均值与标准差。

用法：
    cd model-service
    python ../tools/compare_resample.py
"""
from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
SERVICE = PROJECT_ROOT / "model-service"
PYTHON = sys.executable
DATA_ROOT = "../datasets/aptos2019_224x224"
OUT_DIR = SERVICE / "experiments"

NAMES = ["无DR", "轻度NPDR", "中度NPDR", "重度NPDR", "增殖性PDR"]

#: (标签, 额外参数)
CONFIGS = [
    ("baseline", ["--resample", "none", "--weight-scheme", "inverse"]),
    ("resample_sqrt", ["--resample", "sqrt", "--weight-scheme", "none"]),
    ("resample_oversample", ["--resample", "oversample", "--weight-scheme", "none"]),
]

SEEDS = [42, 0, 1]


def train_once(label: str, extra: list[str], seed: int) -> dict:
    log = OUT_DIR / f"train_{label}_seed{seed}.log"
    cmd = [
        PYTHON, "-m", "training.train",
        "--data-root", DATA_ROOT,
        "--epochs", "30", "--batch-size", "32",
        "--seed", str(seed), *extra,
    ]
    with log.open("w", encoding="utf-8") as fh:
        subprocess.run(cmd, cwd=SERVICE, stdout=fh, stderr=subprocess.STDOUT, check=False)

    metrics = json.loads((SERVICE / "models" / "train_metrics.json").read_text(encoding="utf-8"))
    (OUT_DIR / f"metrics_{label}_seed{seed}.json").write_text(
        json.dumps(metrics, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    return metrics


def mean_std(values: list[float]) -> tuple[float, float]:
    n = len(values)
    m = sum(values) / n
    var = sum((v - m) ** 2 for v in values) / n
    return m, var ** 0.5


def main() -> None:
    OUT_DIR.mkdir(exist_ok=True)
    results: dict[str, list[dict]] = {}

    for label, extra in CONFIGS:
        print(f"\n{'=' * 62}\n配置: {label}  ({' '.join(extra)})\n{'=' * 62}")
        runs = []
        for seed in SEEDS:
            m = train_once(label, extra, seed)
            runs.append(m)
            print(
                f"  seed={seed:<3} acc={m['test_acc']:.4f} macroF1={m['test_macro_f1']:.4f} "
                f"L3F1={m['test_f1_per_class'][3]:.4f}"
            )
        results[label] = runs

    print(f"\n\n{'=' * 78}")
    print("汇总（3 个随机种子的均值 ± 标准差）")
    print(f"{'=' * 78}")
    header = f"{'配置':<22}{'test_acc':>16}{'test_macroF1':>16}{'重度NPDR F1':>16}"
    print(header)
    print("-" * len(header))
    for label, _ in CONFIGS:
        acc_m, acc_s = mean_std([r["test_acc"] for r in results[label]])
        f1_m, f1_s = mean_std([r["test_macro_f1"] for r in results[label]])
        l3_m, l3_s = mean_std([r["test_f1_per_class"][3] for r in results[label]])
        print(f"{label:<22}{acc_m:>9.4f}±{acc_s:.4f}{f1_m:>9.4f}±{f1_s:.4f}{l3_m:>9.4f}±{l3_s:.4f}")

    print("\n逐类 F1 均值：")
    print(f"{'配置':<22}" + "".join(f"{n:>12}" for n in NAMES))
    for label, _ in CONFIGS:
        row = [mean_std([r["test_f1_per_class"][i] for r in results[label]])[0] for i in range(5)]
        print(f"{label:<22}" + "".join(f"{v:>12.4f}" for v in row))

    summary = {
        label: {
            "test_acc_mean": mean_std([r["test_acc"] for r in runs])[0],
            "test_acc_std": mean_std([r["test_acc"] for r in runs])[1],
            "test_macro_f1_mean": mean_std([r["test_macro_f1"] for r in runs])[0],
            "test_macro_f1_std": mean_std([r["test_macro_f1"] for r in runs])[1],
            "per_class_f1_mean": [
                mean_std([r["test_f1_per_class"][i] for r in runs])[0] for i in range(5)
            ],
            "per_class_f1_std": [
                mean_std([r["test_f1_per_class"][i] for r in runs])[1] for i in range(5)
            ],
            "seeds": SEEDS,
        }
        for label, runs in results.items()
    }
    (OUT_DIR / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"\n汇总已写出 -> {OUT_DIR / 'summary.json'}")


if __name__ == "__main__":
    main()
