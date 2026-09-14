"""Grad-CAM 热力图生成（自实现，不依赖 opencv / pytorch-grad-cam）。

实现要点：
- 通过 torch 前向/反向 hook 获取目标卷积层激活与梯度；
- 通道权重 = 梯度空间均值，加权求和后 ReLU 得到类激活图；
- 用 numpy 实现 jet 伪彩色，与原始图像 alpha 混合，输出 PNG 至 heatmaps/。

目标层取骨干 features 中最后一个卷积层（输出为 4D 空间特征）。
"""
from __future__ import annotations

from io import BytesIO
from pathlib import Path
from typing import Tuple

import numpy as np
import torch
import torch.nn as nn
from PIL import Image

from .config import DEVICE, HEATMAPS_DIR, LEVEL_CODES
from .model_loader import get_model
from .preprocess import load_image, to_tensor


def find_target_layer(model: nn.Module) -> nn.Conv2d:
    """返回 features 中最后一个卷积层（空间特征，4D 输出）。"""
    last_conv: nn.Conv2d | None = None
    for mod in model.features.modules():
        if isinstance(mod, nn.Conv2d):
            last_conv = mod
    if last_conv is None:
        raise ValueError("未在 model.features 中找到卷积层")
    return last_conv


def _jet_colormap(gray: np.ndarray) -> np.ndarray:
    """简易 jet 伪彩色映射，gray 为 [0,1] 的二维数组，返回 HxWx3 的 [0,1] RGB。"""
    x = np.clip(gray, 0.0, 1.0)
    r = np.clip(1.5 - np.abs(4.0 * x - 3.0), 0.0, 1.0)
    g = np.clip(1.5 - np.abs(4.0 * x - 2.0), 0.0, 1.0)
    b = np.clip(1.5 - np.abs(4.0 * x - 1.0), 0.0, 1.0)
    return np.stack([r, g, b], axis=-1)


def generate_gradcam(data: bytes, record_id: str) -> Tuple[Path, dict]:
    """生成指定图片的 Grad-CAM 热力图，保存 PNG 并返回路径与命中信息。"""
    model, _ = get_model()
    img = load_image(data)
    x = to_tensor(img)

    target_layer = find_target_layer(model)
    activations: list[torch.Tensor] = []
    gradients: list[torch.Tensor] = []

    def _forward_hook(_m, _inp, out):
        activations.append(out.detach())

    def _backward_hook(_m, _gin, gout):
        # gout[0]: 该层输出对损失的反向梯度，形状 (1, C, H, W)
        gradients.append(gout[0].detach())

    h_fwd = target_layer.register_forward_hook(_forward_hook)
    h_bwd = target_layer.register_full_backward_hook(_backward_hook)

    try:
        model.zero_grad(set_to_none=True)
        x = x.clone().requires_grad_(True)
        out = model(x)
        probs = torch.softmax(out, dim=1)[0]
        idx = int(torch.argmax(probs).item())
        score = out[0, idx]
        score.backward()

        act = activations[0]  # (1, C, H, W)
        grad = gradients[0]  # (1, C, H, W)
        weights = grad.mean(dim=(2, 3), keepdim=True)  # (1, C, 1, 1)
        cam = (weights * act).sum(dim=1, keepdim=True)  # (1, 1, H, W)
        cam = torch.relu(cam)[0, 0].cpu().numpy().astype(np.float32)

        cam -= cam.min()
        if cam.max() > 0:
            cam /= cam.max()

        # 上采样到原图尺寸
        cam_pil = Image.fromarray((cam * 255).astype(np.uint8)).resize(
            img.size, Image.BILINEAR
        )
        cam_norm = np.asarray(cam_pil).astype(np.float32) / 255.0

        heat = _jet_colormap(cam_norm)
        orig = np.asarray(img).astype(np.float32) / 255.0
        blended = np.clip(orig * 0.6 + heat * 0.4, 0.0, 1.0)
        result = Image.fromarray((blended * 255).astype(np.uint8))

        HEATMAPS_DIR.mkdir(parents=True, exist_ok=True)
        out_path = HEATMAPS_DIR / f"{record_id}.png"
        result.save(out_path, format="PNG")
    finally:
        h_fwd.remove()
        h_bwd.remove()

    info = {
        "target_level": LEVEL_CODES[idx],
        "confidence": round(float(probs[idx].item()), 6),
    }
    return out_path, info
