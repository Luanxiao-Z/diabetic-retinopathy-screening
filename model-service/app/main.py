"""DR 智能筛查模型服务（FastAPI）。

提供接口（独立部署，不走 /api/v1）：
- GET  /health  健康检查（Docker 探针；返回设备/是否训练等信息）
- POST /predict 多分类推理，返回分级、置信度、各分级概率、转诊建议
- POST /cam     生成 Grad-CAM 热力图（PNG），阶段 4 将改为上传 MinIO
"""
from __future__ import annotations

import time
import uuid

import torch
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import FileResponse

from . import schemas
from .config import MODEL_VERSION, DEVICE
from .gradcam import generate_gradcam
from .inference import predict_image
from .model_loader import get_model

app = FastAPI(title="DR Screening Model Service", version="0.1.0")


@app.on_event("startup")
def _startup() -> None:
    """预热模型（进程内单例），避免首请求延迟与重复占显存。"""
    get_model()


@app.get("/health", response_model=schemas.HealthResponse)
def health() -> schemas.HealthResponse:
    _, meta = get_model()
    return schemas.HealthResponse(
        status="UP",
        model=meta.get("version", MODEL_VERSION),
        device=DEVICE,
        cuda_available=torch.cuda.is_available(),
        trained=meta.get("trained", False),
        ts=int(time.time()),
    )


def _validate_image(upload: UploadFile) -> None:
    if not upload.content_type or not upload.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="仅支持图片文件")


@app.post("/predict", response_model=schemas.PredictResponse)
async def predict(image: UploadFile = File(...)) -> schemas.PredictResponse:
    _validate_image(image)
    data = await image.read()
    try:
        result = predict_image(data)
    except ValueError as exc:  # 图像损坏/无法解码
        raise HTTPException(status_code=400, detail=f"图像无效: {exc}")
    except Exception as exc:  # 推理异常 → 503 便于后端降级
        raise HTTPException(status_code=503, detail=f"推理失败: {exc}")
    return schemas.PredictResponse(record_id=uuid.uuid4().hex, **result)


@app.post("/cam")
async def cam(image: UploadFile = File(...)):
    _validate_image(image)
    data = await image.read()
    record_id = uuid.uuid4().hex
    try:
        path, info = generate_gradcam(data, record_id)
    except Exception as exc:
        raise HTTPException(status_code=503, detail=f"热力图生成失败: {exc}")
    return FileResponse(
        str(path),
        media_type="image/png",
        filename=f"{record_id}.png",
        headers={
            "X-Record-Id": record_id,
            "X-Target-Level": info["target_level"],
            "X-Confidence": str(info["confidence"]),
        },
    )
