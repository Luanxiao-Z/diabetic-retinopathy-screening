"""模型服务配置与领域常量。

集中维护：模型结构参数、设备选择、DR 分级/转诊字典映射、ImageNet 归一化参数。
设备默认优先 CUDA（本地 RTX 4050 加速），不可用时回退 CPU。
"""
from __future__ import annotations

from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
MODELS_DIR = BASE_DIR / "models"
HEATMAPS_DIR = BASE_DIR / "heatmaps"

# ---- 模型结构 ----
NUM_CLASSES = 5
IMG_SIZE = 224
BACKBONE = "mobilenet_v3_small"  # 备选: efficientnet_b0
# 训练完成后导出 best_model.pth；缺失则随机初始化（未经训练，仅用于管线联调）
MODEL_WEIGHTS_PATH = MODELS_DIR / "best_model.pth"
# 权重加载成功时对外标识的版本；权重缺失/损坏回退随机初始化时用 UNTRAINED 标识
MODEL_VERSION = "aptos2019-mobilenetv3s-1.0.0"
MODEL_VERSION_UNTRAINED = "dev-untrained-0.1.0"

# ---- DR 分级与转诊字典（与后端 B_DR_LEVEL / B_DR_SUGGESTION 保持一致）----
LEVEL_CODES = [f"LEVEL_{i}" for i in range(NUM_CLASSES)]
LEVEL_LABELS = {
    "LEVEL_0": "正常(Normal)",
    "LEVEL_1": "轻度 NPDR",
    "LEVEL_2": "中度 NPDR",
    "LEVEL_3": "重度 NPDR",
    "LEVEL_4": "PDR",
}
SUGGESTION_MAP = {
    "LEVEL_0": "REVIEW",
    "LEVEL_1": "REVIEW",
    "LEVEL_2": "CLINIC",
    "LEVEL_3": "REFERRAL",
    "LEVEL_4": "REFERRAL",
}
SUGGESTION_LABELS = {
    "REVIEW": "定期复查",
    "CLINIC": "建议眼科就诊",
    "REFERRAL": "建议尽快转诊上级医院",
}

# ---- 设备选择：优先 CUDA，否则 CPU 兜底 ----
def select_device() -> str:
    try:
        import torch

        if torch.cuda.is_available():
            return "cuda"
    except Exception:
        pass
    return "cpu"


DEVICE = select_device()

# ---- ImageNet 归一化（与训练保持一致）----
IMAGENET_MEAN = [0.485, 0.456, 0.406]
IMAGENET_STD = [0.229, 0.224, 0.225]
