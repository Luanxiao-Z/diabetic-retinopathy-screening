"""推理管线：字节图像 → 分级结果。

流程：load_image → to_tensor → 前向 → softmax → argmax → 分级编码/标签 → 转诊建议 → 置信度/不确定性。
"""
from __future__ import annotations

import math

import torch
import torch.nn.functional as F

from .config import LEVEL_CODES, LEVEL_LABELS, SUGGESTION_MAP
from .model_loader import get_model
from .preprocess import load_image, to_tensor

# 归一化熵的分母：ln(类别数)
_LN_CLASSES = math.log(len(LEVEL_CODES))


def normalized_entropy(probs: list[float]) -> float:
    """归一化预测熵：0 表示完全确定，1 表示五类均匀分布。

    用途：作为**不确定性指标**。裸 top1 概率（即 confidence）在深度网络上普遍饱和，
    实测 30 张影像中有 20 张 ≥ 0.99，几乎失去区分度；而熵利用了完整的概率分布形状，
    对"高置信度的错误预测"更敏感（实测错分样本熵为 0.36/0.37，明显高于正确样本的 ≤0.22）。
    """
    h = -sum(p * math.log(p) for p in probs if p > 0)
    # max(..., 0.0) 用于消除 one-hot 场景下的负零（-0.0）
    return round(max(h, 0.0) / _LN_CLASSES, 6) if _LN_CLASSES > 0 else 0.0


def predict_image(data: bytes) -> dict:
    """对单张眼底图执行推理，返回结构化结果（不含 record_id）。"""
    model, meta = get_model()
    img = load_image(data)  # 校验图像有效性
    x = to_tensor(img)

    with torch.no_grad():
        logits = model(x)
        probs = F.softmax(logits, dim=1)[0]

    probs_list = probs.cpu().tolist()
    idx = int(torch.argmax(probs).item())
    level = LEVEL_CODES[idx]

    return {
        "result_level": level,
        "result_label": LEVEL_LABELS[level],
        "confidence": round(float(probs_list[idx]), 6),
        "uncertainty": normalized_entropy(probs_list),
        "probabilities": {
            LEVEL_CODES[i]: round(float(probs_list[i]), 6) for i in range(len(LEVEL_CODES))
        },
        "suggestion": SUGGESTION_MAP[level],
        "model_version": meta.get("version", "unknown"),
    }
