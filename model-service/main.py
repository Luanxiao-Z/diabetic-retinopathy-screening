"""DR 智能筛查模型服务（FastAPI 骨架，阶段 0 占位）。

阶段 3 将在此接入 PyTorch + timm 训练的 MobileNetV3/EfficientNet-B0 模型，
并实现 Grad-CAM 热力图生成。本期仅提供 /health 与 /predict 占位实现，
返回确定性的模拟分级结果，便于前后端联调。
"""
from __future__ import annotations

import time
import uuid
from typing import Any

from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel

app = FastAPI(title="DR Screening Model Service", version="0.1.0")


class PredictResponse(BaseModel):
    record_id: str
    result_level: str
    result_label: str
    confidence: float
    probabilities: dict[str, float]
    suggestion: str
    model_version: str


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
MODEL_VERSION = "mock-0.1.0"


@app.get("/health")
def health() -> dict[str, Any]:
    return {"status": "UP", "model": MODEL_VERSION, "ts": int(time.time())}


@app.post("/predict", response_model=PredictResponse)
async def predict(image: UploadFile = File(...)) -> PredictResponse:
    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="仅支持图片文件")

    # 占位逻辑：阶段 3 替换为真实模型推理（读取图片 -> 预处理 -> 前向 -> 后处理）。
    _ = await image.read()
    record_id = uuid.uuid4().hex

    # 模拟确定性结果（仅用于联调，不具临床意义）
    result_level = "LEVEL_2"
    confidence = 0.0
    probabilities = {lvl: 0.0 for lvl in LEVEL_LABELS}
    probabilities[result_level] = 0.87

    return PredictResponse(
        record_id=record_id,
        result_level=result_level,
        result_label=LEVEL_LABELS[result_level],
        confidence=confidence,
        probabilities=probabilities,
        suggestion=SUGGESTION_MAP[result_level],
        model_version=MODEL_VERSION,
    )
