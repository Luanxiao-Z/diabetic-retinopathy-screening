# 糖尿病视网膜病变（DR）智能筛查系统

基于 `spec/` 规范体系与 `docs/项目开发计划.md`（v1.1 决策冻结版）实现的工程骨架。当前处于 **阶段 0：工程脚手架与基础设施**，已完成后端多模块、前端 PC 端、模型服务骨架、部署与数据库初始化脚本，并通过编译/构建校验。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.4（Java 21）、Maven 多模块、MyBatis、Redis（Token Session） |
| 前端 | Vue 3 + TypeScript + Vite 6 + Element Plus + Pinia + Vue Router + Axios（Yarn） |
| 模型服务 | Python 3.10+ / FastAPI（阶段 3 接入 PyTorch + timm + Grad-CAM） |
| 基础设施 | MySQL 8 / Redis 7 / MinIO（Docker 预留） |

## 目录结构

```
backend/               Maven 聚合工程（父 pom + drs-dependencies-bom + drs-module-* + drs-app）
frontend/web/          Vue3 + Vite PC 端
model-service/        FastAPI 模型服务骨架
deploy/                docker-compose.yml / nginx / docker/Dockerfile.*
sql/init/              MySQL 建表与字典初始化脚本
docs/                  项目综述、API 文档、开发计划
tools/mvn.sh           Git Bash 下 Maven 包装脚本（解决 POSIX 路径问题）
```

## 环境要求

- JDK 21、Maven 3.9.x
- Node.js 18+（推荐 22）、Yarn 1.22
- Python 3.10+
- Docker（可选，仅用于基础设施；本期交付形态为纯本地运行，Docker 配置作预留）

## 快速开始

### 1. 基础设施（MySQL / Redis / MinIO）

本期默认本地运行，可跳过 Docker。若需一键起基础设施：

```bash
docker compose -f deploy/docker-compose.yml up -d
```

连接信息：`dr_screening / root:root123`（MySQL 3306）、`redis:6379`、`minio:9000/9001`（minioadmin/minioadmin）。`sql/init` 在 MySQL 首次启动后自动执行建表与字典初始化。

### 2. 后端

> **Git Bash 路径说明**：Windows 版 `java.exe` 无法识别 Git Bash 的 POSIX 路径，仓库内置 `tools/mvn.sh` 通过 `cygpath -m` 转换。请勿在 Git Bash 中直接调用 `mvn`，使用包装脚本：

```bash
# 编译全部模块
bash tools/mvn.sh compile

# 启动后端（聚合启动模块 drs-app-server，端口 8080）
bash tools/mvn.sh -pl drs-app/drs-app-server -am spring-boot:run
```

### 3. 模型服务

```bash
cd model-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
# 健康检查：GET http://localhost:8000/health
```

阶段 0 仅提供确定性占位推理（返回 LEVEL_2 模拟结果），用于前后端联调；阶段 3 替换为真实模型推理与 Grad-CAM 热力图。

### 4. 前端（已配置国内镜像）

`frontend/web/.yarnrc` 已将 registry 指向 `https://registry.npmmirror.com`，加速依赖安装。

```bash
cd frontend/web
yarn install      # 国内镜像
yarn dev          # 开发服务器 http://localhost:5173（/api 代理至后端 8080）
yarn build        # 生产构建，产物位于 dist/
```

## 默认端口

| 服务 | 端口 |
| --- | --- |
| 后端 backend | 8080 |
| 模型服务 model-service | 8000 |
| 前端 web（dev） | 5173 |
| MySQL | 3306 |
| Redis | 6379 |
| MinIO API / Console | 9000 / 9001 |

## 阶段进度

- [x] 阶段 0 脚手架与基础设施（后端编译通过、前端构建通过、部署与 SQL 脚本就绪）
- [ ] 阶段 1 数据层（Mapper/Entity/Convert 落地）
- [ ] 阶段 2 认证与权限（AuthFilter 联动 Redis、@RequirePermission 拦截器对接 system 模块）
- [ ] 阶段 3 模型服务（模型训练 + Grad-CAM）
- [ ] 阶段 4 业务核心（筛查上传→推理→落库→统计→导出）
- [ ] 阶段 5 前端 PC 业务页面
- [ ] 阶段 6 H5（后续可选）
- [ ] 阶段 7 部署（本地运行 + Docker 预留）
