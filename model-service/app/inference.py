"""推理管线：字节图像 → 分级结果。

流程：load_image → to_tensor → 前向 → softmax → argmax → 分级编码/标签 → 转诊建议 → 置信度。
"""
from __future__ import annotations

import torch
import torch.nn.functional as F

from .config import LEVEL_CODES, LEVEL_LABELS, SUGGESTION_MAP
from .model_loader import get_model
from .preprocess import load_image, to_tensor


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
        "probabilities": {
            LEVEL_CODES[i]: round(float(probs_list[i]), 6) for i in range(len(LEVEL_CODES))
        },
        "suggestion": SUGGESTION_MAP[level],
        "model_version": meta.get("version", "unknown"),
    }
