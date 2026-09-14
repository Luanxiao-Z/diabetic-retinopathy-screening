# 糖尿病视网膜病变（DR）智能筛查系统

基于 `spec/` 规范体系与 `docs/项目开发计划.md`（v1.1 决策冻结版）实现的工程骨架。**阶段 0（脚手架）、阶段 1（数据层 + 认证接口）、阶段 2（认证与权限）、阶段 3（模型服务）与阶段 4（业务核心）已完成**：后端多模块编译/构建通过、前端构建通过、MySQL 建表与字典初始化已落库、登录 / 字典 / 当前用户接口端到端验证可用；阶段 2 新增管理员用户与字典 CRUD 接口并接入 `@RequirePermission`，角色—权限映射与数据权限 SELF/ALL 落地，401/403 鉴权端到端验证通过；阶段 3 实现 FastAPI 模型服务（MobileNetV3-Small 5 类推理管线、自实现 Grad-CAM 热力图、/predict 与 /cam 接口，CUDA cu130 版 PyTorch 跑在本地 RTX 4050 上）。**本期使用未经训练的随机初始化模型**（权重缺失时自动回退），用于打通管线与联调；模型训练推迟至最后阶段实现，训练完成后导出 `models/best_model.pth` 即可零代码切换。

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
- Python 3.12+
- Docker（可选，仅用于基础设施；本期交付形态为纯本地运行，Docker 配置作预留）

## 快速开始

### 1. 基础设施（MySQL / Redis / MinIO）

本期默认本地运行，可跳过 Docker。若需一键起基础设施：

```bash
docker compose -f deploy/docker-compose.yml up -d
```

连接信息：MySQL 3306（业务库 `dr_screening`）、Redis 26379、MinIO `9000`/`9001`；数据库与中间件账号、密码均通过环境变量注入（如 `DB_PASSWORD` / `REDIS_PASSWORD` / `MINIO_SECRET_KEY`），**请勿将明文写入版本库**。`sql/init` 在 MySQL 首次启动后执行建表与字典初始化。

### 0. 运行所需环境变量

后端 `application.yml` 中的敏感项均通过环境变量注入，启动前请在终端设置（或写入用户级环境变量，**切勿入库**）：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | MySQL 密码（应用以 `root` 连接业务库 `dr_screening`） |
| `REDIS_PASSWORD` | Redis 密码（Docker 容器 `fjzhmz-redis`，端口 26379） |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 凭据（阶段 4 用于眼底图与热力图私有桶存储） |

> 首次启动会由 `DataInitializer` 自动播种账号：**`admin` / `admin123`**（角色 ADMIN，数据权限 ALL）与 **`doctor` / `doctor123`**（角色 DOCTOR，数据权限 SELF），用于演示与鉴权联调；账号已存在则跳过（幂等）。

### 2. 后端

> **Git Bash 路径说明**：Windows 版 `java.exe` 无法识别 Git Bash 的 POSIX 路径，仓库内置 `tools/mvn.sh` 通过 `cygpath -m` 转换。请勿在 Git Bash 中直接调用 `mvn`，使用包装脚本。

```bash
# 1) 全量编译并打可执行 fat-jar（已含依赖，约 40MB）
bash tools/mvn.sh clean install -DskipTests

# 2) 进入 jar 所在目录后用相对文件名启动（规避中文路径 jar 参数问题）
cd backend/drs-app/drs-app-server/target
java -jar drs-app-server-1.0.0.jar --server.port=8080
```

> 开发模式亦可用 `bash tools/mvn.sh -pl drs-app/drs-app-server -am spring-boot:run`（需在该模块上执行，而非聚合根）。若所在终端被代理注入 `SERVER__PORT` 导致端口异常，显式加 `--server.port=8080` 覆盖即可。

### 3. 模型服务（FastAPI，独立端口 8000）

> 依赖含 CUDA 13.0 (cu130) 版 PyTorch，本地 RTX 4050 可加速训练/推理；无 NVIDIA GPU 环境请把 `requirements.txt` 末两行改为 `+cpu` 轮子后再安装。

```bash
cd model-service
python -m venv .venv && source .venv/bin/activate   # 推荐虚拟环境
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
# 健康检查：GET  http://localhost:8000/health
# 推理：      POST http://localhost:8000/predict  (multipart/form-data: image=眼底图)
# 热力图：    POST http://localhost:8000/cam       (multipart/form-data: image=眼底图，返回 PNG)
```

> 本期使用**未经训练的随机初始化模型**（`models/best_model.pth` 缺失时自动回退），仅用于打通推理管线、Grad-CAM 与前后端联调；**模型训练推迟至最后阶段实现**，训练完成后导出 `models/best_model.pth` 即可零代码切换。

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
| Redis | 26379 |
| MinIO API / Console | 9000 / 9001 |

## 阶段 1 接口速览（已验证）

基础路径 `/api/v1`，响应体统一为 `{code, msg, data}`（`code=200` 成功）。登录令牌通过请求头 `X-Access-Token` 传递。

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/auth/sessions` | 登录（admin/admin123），返回 token + 权限集 + 数据权限 | 否 |
| DELETE | `/auth/sessions` | 登出（使 Redis 中的 token 失效） | 是 |
| GET | `/common/dicts/{domainCode}/items` | 查询某字典域下的字典项 | 是 |
| GET | `/common/dicts` | 字典域列表 | 是 |
| GET | `/common/users/me/permissions` | 当前用户权限集 | 是 |
| GET | `/common/users/me` | 当前用户资料（用户名/角色/数据权限） | 是 |

> 未携带/携带失效 token 访问鉴权接口返回 `code=401`；`DataInitializer` 在 MySQL 就绪后自动播种 `admin`。

## 阶段 2 接口速览（已验证）

在阶段 1 的基础上，新增**管理员域**（`/api/v1/admin`）接口并接入 `@RequirePermission` 功能权限拦截；角色—权限映射与数据权限 SELF/ALL 已落地。响应体仍统一为 `{code, msg, data}`，鉴权失败时同时返回正确的 HTTP 状态码（`401` 未登录 / `403` 无权限）。

| 方法 | 路径 | 说明 | 所需权限 |
| --- | --- | --- | --- |
| GET | `/admin/users` | 用户分页列表 | `admin:user:view` |
| GET | `/admin/users/{id}` | 用户详情 | `admin:user:view` |
| POST | `/admin/users` | 创建用户（返回 201） | `admin:user:edit` |
| PUT | `/admin/users/{id}` | 修改用户 | `admin:user:edit` |
| DELETE | `/admin/users/{id}` | 删除用户（逻辑删除） | `admin:user:edit` |
| PATCH | `/admin/users/{id}/state` | 启停用户 | `admin:user:edit` |
| GET | `/admin/dict-domains` | 字典域列表 | `admin:dict:view` |
| POST | `/admin/dict-domains` | 创建字典域（返回 201） | `admin:dict:edit` |
| PUT | `/admin/dict-domains/{domainCode}` | 修改字典域 | `admin:dict:edit` |
| DELETE | `/admin/dict-domains/{domainCode}` | 删除字典域（级联删除其下字典项） | `admin:dict:edit` |
| GET | `/admin/dict-domains/{domainCode}/items` | 字典项列表 | `admin:dict:view` |
| POST | `/admin/dict-domains/{domainCode}/items` | 创建字典项（返回 201） | `admin:dict:edit` |
| PUT | `/admin/dict-domains/{domainCode}/items/{itemCode}` | 修改字典项 | `admin:dict:edit` |
| DELETE | `/admin/dict-domains/{domainCode}/items/{itemCode}` | 删除字典项 | `admin:dict:edit` |

> 角色权限矩阵（轻量化，无 RBAC 表）：`ADMIN` 拥有全部 `admin:*` 与 `common:*` 权限、数据权限 `ALL`；`DOCTOR` 仅拥有 `common:dict:view` 与 `biz:screening:*`，数据权限 `SELF`。以 `DOCTOR` 令牌访问 `/admin/**` 返回 `HTTP 403`。

## 阶段 3 接口速览（模型服务，独立部署不走 `/api/v1`）

模型服务独立部署于 8000 端口，后端经 HTTP 调用并在失败/超时时降级（返回 503）。响应契约 `PredictResponse`：

```json
{
  "record_id": "uuid",
  "result_level": "LEVEL_2",
  "result_label": "中度 NPDR",
  "confidence": 0.873,
  "probabilities": {"LEVEL_0": 0.01, "LEVEL_1": 0.02, "LEVEL_2": 0.873, "LEVEL_3": 0.05, "LEVEL_4": 0.047},
  "suggestion": "CLINIC",
  "model_version": "dev-untrained-0.1.0"
}
```

| 方法 | 路径 | 说明 | 请求 |
| --- | --- | --- | --- |
| GET | `/health` | 健康检查（Docker 探针）；返回 `status / model / device / cuda_available / trained` | — |
| POST | `/predict` | 多分类推理：返回分级编码、标签、置信度、各分级概率、转诊建议 | `multipart/form-data: image=眼底图` |
| POST | `/cam` | 生成 Grad-CAM 热力图，返回叠加 PNG（同时以响应头返回 `X-Record-Id` / `X-Target-Level` / `X-Confidence`） | `multipart/form-data: image=眼底图` |

> 分级与转诊映射（与后端 `B_DR_LEVEL` / `B_DR_SUGGESTION` 一致，业务层维护）：`LEVEL_0/1 → REVIEW`（定期复查）、`LEVEL_2 → CLINIC`（建议眼科就诊）、`LEVEL_3/4 → REFERRAL`（建议尽快转诊）。本期模型未经训练，结果为随机初始化权重输出，仅验证管线；阶段 4 已实现热力图上传统 MinIO 并落库 `grad_cam_key`。

## 阶段 4 接口速览（业务核心，已验证编译/打包）

基础路径 `/api/v1`，响应体统一为 `{code, msg, data}`；所有接口均需 `X-Access-Token`。筛查记录落库表 `biz_screening_record`（见 `sql/init/01_init_dr_screening.sql`）。

| 方法 | 路径 | 说明 | 所需权限 |
| --- | --- | --- | --- |
| POST | `/biz/screening-records` | 批量上传眼底图：`multipart` 多文件 + 患者信息 → 上传 MinIO → 调模型服务推理 → 落库（含热力图） | `biz:screening:create` |
| GET | `/biz/screening-records` | 分页查询（患者名模糊 / 分级 / 起止时间），受数据权限 SELF/ALL 约束 | `biz:screening:view` |
| GET | `/biz/screening-records/{id}` | 详情（含图片与热力图预签名 URL） | `biz:screening:view` |
| DELETE | `/biz/screening-records/{id}` | 删除（逻辑删除 + 清理 MinIO 对象；管理员或记录创建者） | `biz:screening:delete` |
| GET | `/biz/screening-records/statistics` | 统计：各级/建议分布、转诊率、近 30 天趋势 | `biz:screening:view` |
| GET | `/biz/screening-records/exports` | 导出 Excel（按 ids 或当前筛选条件） | `biz:screening:export` |

> 权限矩阵（轻量化，无 RBAC 表）：`DOCTOR` 拥有 `biz:screening:create/view/export/delete` 与 `common:dict:view`，数据权限 `SELF`（仅查本人记录）；`ADMIN` 额外拥有全部 `admin:*`，数据权限 `ALL`。对象存储采用 MinIO Java 客户端（S3 兼容），私有桶 `dr-screening`，图片/热力图以预签名 URL（30 分钟时效）返回前端；模型服务不可达时推理与热力图生成降级（热力图失败不阻断主流程）。Excel 导出仅导出 Excel（见 spec 约定）。

## 阶段进度

- [x] 阶段 0 脚手架与基础设施（后端编译通过、前端构建通过、部署与 SQL 脚本就绪）
- [x] 阶段 1 数据层与认证接口（MyBatis-Plus 配置、Entity/Mapper/Convert/DTO/VO、登录/字典/当前用户接口、AuthFilter + Redis Token、建表落库并端到端验证）
- [x] 阶段 2 认证与权限（@RequirePermission 拦截器对接业务接口、管理员用户/字典 CRUD、角色—权限映射与数据权限 SELF/ALL 落地、401/403 鉴权端到端验证）
- [x] 阶段 3 模型服务（FastAPI 推理管线 + 自实现 Grad-CAM + /predict、/cam、/health 接口；CUDA cu130 跑在 RTX 4050，使用未经训练模型验证管线）
- [x] 阶段 4 业务核心（筛查上传→推理→落库→统计→导出：MinIO 私有桶存储、模型服务 HTTP 调用、数据权限 SELF/ALL、Excel 导出；后端 compile/package 验证通过）
- [ ] 模型训练（推迟至最后阶段：APTOS 2019 训练 MobileNetV3-Small、导出 `models/best_model.pth` 后零代码切换）
- [ ] 阶段 5 前端 PC 业务页面
- [ ] 阶段 6 H5（后续可选）
- [ ] 阶段 7 部署（本地运行 + Docker 预留）
