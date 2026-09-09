# 糖尿病视网膜病变（DR）智能筛查系统

基于 `spec/` 规范体系与 `docs/项目开发计划.md`（v1.1 决策冻结版）实现的工程骨架。**阶段 0（脚手架）与阶段 1（数据层 + 认证接口）已完成**：后端多模块编译/构建通过、前端构建通过、MySQL 建表与字典初始化已落库、登录 / 字典 / 当前用户接口端到端验证可用。

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

连接信息：MySQL 3306（业务库 `dr_screening`）、Redis 26379、MinIO `9000`/`9001`；数据库与中间件账号、密码均通过环境变量注入（如 `DB_PASSWORD` / `REDIS_PASSWORD` / `MINIO_SECRET_KEY`），**请勿将明文写入版本库**。`sql/init` 在 MySQL 首次启动后执行建表与字典初始化。

### 0. 运行所需环境变量

后端 `application.yml` 中的敏感项均通过环境变量注入，启动前请在终端设置（或写入用户级环境变量，**切勿入库**）：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | MySQL 密码（应用以 `root` 连接业务库 `dr_screening`） |
| `REDIS_PASSWORD` | Redis 密码（Docker 容器 `fjzhmz-redis`，端口 26379） |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 凭据（阶段 1 仅预留，上传接口后续阶段实现） |

> 首次启动会由 `DataInitializer` 自动播种管理员账号 **`admin` / `admin123`**（角色 ADMIN，数据权限 ALL）。

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

## 阶段进度

- [x] 阶段 0 脚手架与基础设施（后端编译通过、前端构建通过、部署与 SQL 脚本就绪）
- [x] 阶段 1 数据层与认证接口（MyBatis-Plus 配置、Entity/Mapper/Convert/DTO/VO、登录/字典/当前用户接口、AuthFilter + Redis Token、建表落库并端到端验证）
- [ ] 阶段 2 认证与权限（@RequirePermission 拦截器对接业务接口、数据权限 SELF/ALL 落地）
- [ ] 阶段 3 模型服务（模型训练 + Grad-CAM）
- [ ] 阶段 4 业务核心（筛查上传→推理→落库→统计→导出）
- [ ] 阶段 5 前端 PC 业务页面
- [ ] 阶段 6 H5（后续可选）
- [ ] 阶段 7 部署（本地运行 + Docker 预留）
