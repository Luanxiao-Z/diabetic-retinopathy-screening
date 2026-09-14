"""图像预处理：字节 → PIL → 归一化张量。

- load_image: 任意图片字节转为 RGB PIL。
- to_tensor: Resize(256) + CenterCrop(224) + ImageNet 归一化，输出 (1,3,224,224) 张量。
"""
from __future__ import annotations

from io import BytesIO

import torch
from PIL import Image
from torchvision import transforms

from .config import IMG_SIZE, IMAGENET_MEAN, IMAGENET_STD, DEVICE

_TRANSFORM = transforms.Compose(
    [
        transforms.Resize(256),
        transforms.CenterCrop(IMG_SIZE),
        transforms.ToTensor(),
        transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD),
    ]
)


def load_image(data: bytes) -> Image.Image:
    img = Image.open(BytesIO(data))
    img = img.convert("RGB")
    return img


def to_tensor(img: Image.Image) -> torch.Tensor:
    tensor = _TRANSFORM(img)
    return tensor.unsqueeze(0).to(DEVICE)
