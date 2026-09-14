# ===========================================================================
# 模型服务容器化模板（预留，阶段 8 部署使用）
# ---------------------------------------------------------------------------
# 说明：
# - 当前 requirements.txt 固定 CUDA 13.0 (cu130) 轮子，需配合支持 GPU 的基础镜像。
#   纯 CPU 运行环境请先编辑 requirements.txt 改为 +cpu 轮子。
# - 实际部署镜像建议基于 nvidia/cuda 运行时（如 nvidia/cuda:13.0.0-runtime-ubuntu22.04）
#   以启用 GPU 加速；本地开发直接用宿主机 Python 运行即可（见 README）。
# - 暴露 8000 端口，入口为 app.main:app（uvicorn）。
# ===========================================================================
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
