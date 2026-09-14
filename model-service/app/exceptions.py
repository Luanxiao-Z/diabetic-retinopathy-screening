"""模型服务自定义异常。

code 字段用于映射到 HTTP 状态码，便于在路由层统一转换。
"""
from __future__ import annotations


class ModelServiceError(Exception):
    code = 500


class ModelLoadError(ModelServiceError):
    code = 503


class InferenceError(ModelServiceError):
    code = 500


class InvalidImageError(ModelServiceError):
    code = 400
