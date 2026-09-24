"""模型构建与加载。

- build_backbone: 构建 MobileNetV3-Small（默认）或 EfficientNet-B0，替换分类头为 5 类。
- load_model: 尝试加载 models/best_model.pth；缺失/损坏则保持随机初始化（未经训练）。
- get_model: 进程内单例，避免重复构建与重复占用显存。
"""
from __future__ import annotations

import logging

import torch
import torch.nn as nn
from torchvision import models

from .config import (
    BACKBONE,
    NUM_CLASSES,
    MODEL_WEIGHTS_PATH,
    MODEL_VERSION,
    MODEL_VERSION_UNTRAINED,
    DEVICE,
)

logger = logging.getLogger("drs.model")

_MODEL: nn.Module | None = None
_META: dict | None = None


def build_backbone(num_classes: int = NUM_CLASSES) -> nn.Module:
    """构建骨干网络并将分类头替换为 num_classes 输出。

    使用 weights=None 进行随机初始化（未经训练），不下载 ImageNet 预训练权重。
    """
    if BACKBONE == "mobilenet_v3_small":
        model = models.mobilenet_v3_small(weights=None)
        head = model.classifier[-1]
        model.classifier[-1] = nn.Linear(head.in_features, num_classes)
    elif BACKBONE == "efficientnet_b0":
        model = models.efficientnet_b0(weights=None)
        head = model.classifier[1]
        model.classifier[1] = nn.Linear(head.in_features, num_classes)
    else:
        raise ValueError(f"不支持的骨干网络: {BACKBONE}")
    return model


def load_model() -> tuple[nn.Module, dict]:
    """加载模型权重；缺失则随机初始化（未经训练，仅用于管线联调）。"""
    model = build_backbone()
    meta = {
        "trained": False,
        "weights_path": str(MODEL_WEIGHTS_PATH),
        "version": MODEL_VERSION_UNTRAINED,
        "backbone": BACKBONE,
    }

    if MODEL_WEIGHTS_PATH.exists():
        try:
            state = torch.load(MODEL_WEIGHTS_PATH, map_location="cpu", weights_only=True)
            if isinstance(state, dict) and "state_dict" in state:
                state = state["state_dict"]
            model.load_state_dict(state, strict=False)
            meta["trained"] = True
            meta["version"] = MODEL_VERSION
            logger.info("已加载权重: %s", MODEL_WEIGHTS_PATH)
        except Exception as exc:  # 权重损坏/格式不符 → 回退随机初始化
            logger.warning("权重加载失败，回退随机初始化: %s", exc)
            meta["trained"] = False
    else:
        logger.warning(
            "权重文件不存在 (%s)，使用未经训练的随机初始化模型（阶段 3 联调用）",
            MODEL_WEIGHTS_PATH,
        )

    model.to(DEVICE)
    model.eval()
    return model, meta


def get_model() -> tuple[nn.Module, dict]:
    """进程内单例：首次调用时构建并加载，后续复用。"""
    global _MODEL, _META
    if _MODEL is None:
        _MODEL, _META = load_model()
    return _MODEL, _META
