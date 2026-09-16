"""DR 筛查模型训练模块。

- dataset：APTOS 2019 数据加载（自动布局探测 / 增强 / 分层抽样 / 类别加权）。
- train：训练入口（与推理服务同构的 MobileNetV3-Small 5 类头，导出 best_model.pth 零代码切换）。
"""
