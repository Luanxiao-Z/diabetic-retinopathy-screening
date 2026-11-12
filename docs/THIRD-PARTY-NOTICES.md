# 第三方组件许可清单

本清单列出项目直接依赖的第三方组件及其许可证，供合规审查与二次分发参考。
许可证以各组件的上游仓库声明为准；若上游发生变更，以变更后的声明为准。

> 本项目自身以 [MIT License](../LICENSE) 发布。以下组件各自遵循其许可证，
> 分发本项目时请一并保留各组件要求的版权声明与许可文本。

## 一、前端（frontend/web）

### 运行时依赖

| 组件 | 版本 | 许可证 | 用途 |
| --- | --- | --- | --- |
| [Vue](https://github.com/vuejs/core) | 3.5.x | MIT | 渐进式前端框架 |
| [Vue Router](https://github.com/vuejs/router) | 4.5.x | MIT | 路由与多标签页 |
| [Pinia](https://github.com/vuejs/pinia) | 2.3.x | MIT | 状态管理 |
| [Element Plus](https://github.com/element-plus/element-plus) | 2.9.x | MIT | UI 组件库 |
| [ECharts](https://github.com/apache/echarts) | 5.5.x | Apache-2.0 | 统计图表 |
| [Axios](https://github.com/axios/axios) | 1.7.x | MIT | HTTP 请求 |

### 构建与检查工具（不随产物分发）

| 组件 | 版本 | 许可证 |
| --- | --- | --- |
| [Vite](https://github.com/vitejs/vite) | 6.0.x | MIT |
| [TypeScript](https://github.com/microsoft/TypeScript) | 5.6.x | Apache-2.0 |
| [vue-tsc](https://github.com/vuejs/language-tools) | 2.2.x | MIT |
| [ESLint](https://github.com/eslint/eslint) | 9.17.x | MIT |
| [Prettier](https://github.com/prettier/prettier) | 3.4.x | MIT |
| [Stylelint](https://github.com/stylelint/stylelint) | 16.13.x | MIT |
| [postcss-html](https://github.com/gucong3000/postcss-html) | 2.0.x | MIT |

## 二、后端（backend）

| 组件 | 版本 | 许可证 | 用途 |
| --- | --- | --- | --- |
| [Spring Boot](https://github.com/spring-projects/spring-boot) | 3.4.4 | Apache-2.0 | 应用框架与自动装配 |
| [Spring Framework](https://github.com/spring-projects/spring-framework) | 6.2.x | Apache-2.0 | IoC / MVC / 事务 |
| [MyBatis-Plus](https://github.com/baomidou/mybatis-plus) | 3.5.11 | Apache-2.0 | ORM 与分页 |
| [Jedis](https://github.com/redis/jedis) | 5.2.0 | MIT | Redis 客户端（令牌会话、验证码、限流） |
| [Knife4j](https://github.com/xiaoymin/knife4j) | 4.5.0 | Apache-2.0 | OpenAPI 文档界面 |
| [Apache POI](https://github.com/apache/poi) | 5.x | Apache-2.0 | Excel 导出 |
| [Project Lombok](https://github.com/projectlombok/lombok) | 1.18.x | MIT | 编译期样板代码生成 |
| [easy-captcha](https://gitee.com/whvse/EasyCaptcha) | 1.6.x | MIT | 图形验证码 |
| [MySQL Connector/J](https://github.com/mysql/mysql-connector-j) | 8.x | GPL-2.0 with Universal FOSS Exception | MySQL 驱动 |

> MySQL Connector/J 采用 GPL-2.0 + FOSS 例外条款：作为开源项目使用不受 GPL 传染性约束，
> 若将本项目改为闭源商业分发，请自行评估该条款或替换驱动。

## 三、模型服务（model-service）

| 组件 | 版本 | 许可证 | 用途 |
| --- | --- | --- | --- |
| [PyTorch](https://github.com/pytorch/pytorch) | 2.14.0 | BSD-3-Clause | 推理与 Grad-CAM |
| [torchvision](https://github.com/pytorch/vision) | 0.29.0 | BSD-3-Clause | 预训练权重与图像变换 |
| [FastAPI](https://github.com/fastapi/fastapi) | ≥0.115 | MIT | 推理服务框架 |
| [Uvicorn](https://github.com/encode/uvicorn) | ≥0.34 | BSD-3-Clause | ASGI 服务器 |
| [NumPy](https://github.com/numpy/numpy) | ≥2.1 | BSD-3-Clause | 数值计算 |
| [Pillow](https://github.com/python-pillow/Pillow) | ≥11.0 | MIT-CMU (HPND) | 图像读写与热力图合成 |
| [python-multipart](https://github.com/Kludex/python-multipart) | ≥0.0.20 | Apache-2.0 | 表单文件上传解析 |

模型骨干网络 MobileNetV3-Small 来自 torchvision，其权重与架构遵循 torchvision 的 BSD-3-Clause。

## 四、基础设施（docker-compose 编排）

| 组件 | 许可证 |
| --- | --- |
| [MySQL](https://github.com/mysql/mysql-server) 8 | GPL-2.0（Server） |
| [Redis](https://github.com/redis/redis) 7 | RSALv2 / SSPLv1（自 7.4 起） |
| [MinIO](https://github.com/minio/minio) / Silo（MinIO 兼容 fork） | AGPL-3.0 |
| [Nginx](https://github.com/nginx/nginx) 1.27 | BSD-2-Clause |

> 基础设施以独立进程运行，与本项目代码不构成衍生作品；如在商业环境部署，
> 请自行核对 Redis（RSALv2/SSPLv1）与 MinIO（AGPL-3.0）的许可要求。

## 五、数据集

| 数据集 | 许可与限制 |
| --- | --- |
| APTOS 2019 Blindness Detection | 版权归原始提供方（Kaggle 竞赛 / Aravind Eye Hospital），**仅限科研与教学用途，不得商用** |

数据集不随仓库分发（`datasets/` 已在 `.gitignore` 中排除），需按 README「十一、数据集」自行下载。

## 六、如何更新本清单

新增或升级依赖时，请同步更新对应表格中的版本与许可证。
前端可参考 `package.json`，后端参考 `backend/pom.xml`，模型服务参考 `model-service/requirements.txt`。
