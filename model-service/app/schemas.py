"""模型服务接口数据契约（Pydantic）。

PredictResponse 与后端约定保持一致：
{record_id, result_level, result_label, confidence, probabilities, suggestion, model_version}
"""
from __future__ import annotations

from typing import Dict

from pydantic import BaseModel


class PredictResponse(BaseModel):
    record_id: str
    result_level: str
    result_label: str
    confidence: float
    probabilities: Dict[str, float]
    suggestion: str
    model_version: str


class HealthResponse(BaseModel):
    status: str
    model: str
    device: str
    cuda_available: bool
    trained: bool
    ts: int
