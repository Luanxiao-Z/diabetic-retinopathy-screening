# 部署说明

本目录提供 **Docker Compose 一键编排**，用于在未安装 MySQL / Redis / 对象存储等中间件的环境里快速启动系统。

编排默认启动 **5 个服务**：基础设施 3 个（MySQL、Redis、对象存储）+ 应用 2 个（后端、前端）。模型服务因镜像含 PyTorch、体积较大（CPU 版约 1.5–2 GB），默认由**宿主机 Python 3.13+ 常驻**提供；需要完整容器化时使用 `--profile model` 追加。

> **对象存储镜像**：MinIO 官方社区镜像已迁出 Docker Hub，编排默认使用 Pigsty 社区维护的 fork **Silo**（`pgsty/silo`）。其 S3 API、`MINIO_*` 环境变量、启动命令、控制台端口与 MinIO 完全兼容，后端代码无需改动；也可通过 `MINIO_IMAGE` 换成其他 S3 兼容实现。

若已自行部署中间件、只需分别启动三个应用，见仓库根 [README.md 第六节](../README.md#六快速开始)。

> **前提**：仅需安装 Docker Engine 与 Docker Compose 插件；JDK、Maven、Node 由镜像内构建阶段提供。模型权重 `model-service/models/best_model.pth` 与 `train_metrics.json` **已纳入版本控制**，克隆仓库后即可直接使用。

---

## 一、目录结构

| 文件 | 用途 |
| --- | --- |
| `docker-compose.yml` | 服务编排、健康检查、依赖顺序、数据卷与网络 |
| `.env.example` | 环境变量模板（复制为 `.env` 后填写；`.env` 不入库） |
| `docker/Dockerfile.backend` | 后端多阶段构建：Maven 3.9 + JDK 21 编译 → JRE 运行 fat-jar |
| `docker/Dockerfile.frontend` | 前端多阶段构建：Node 22 + Yarn 打包 → Nginx 托管 `dist/` |
| `docker/Dockerfile.model` | 模型服务：Python 3.13+、FastAPI、PyTorch（默认 CPU 轮子） |
| `nginx/nginx.conf` | Nginx 配置：SPA 路由回退、`/api` 反代、上传体积限制 |

三个 Dockerfile 的构建上下文均为**仓库根目录**（compose 中的 `context: ..`），因此必须从仓库根目录执行 compose 命令。

---

## 二、一键启动与停止

```bash
# 1) 准备环境变量（务必修改密码，勿使用示例值）
cp deploy/.env.example deploy/.env
$EDITOR deploy/.env

# 2) 构建并后台启动（默认不含模型服务）
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build

# 3) 查看状态与日志
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f backend
```

> **注意变量优先级**：Docker Compose 的取值顺序为 **shell 环境变量 > `--env-file`**。若宿主机已导出 `DB_PASSWORD`、`REDIS_PASSWORD`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`（本地开发时常见），会覆盖 `.env` 中的值，导致「改了 .env 却不生效」。排查密码不一致时，先在当前终端 `unset` 这些变量，或用 `env -u DB_PASSWORD -u REDIS_PASSWORD -u MINIO_ACCESS_KEY -u MINIO_SECRET_KEY docker compose ...` 启动。

启动完成后：

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 系统前端 | `http://localhost:${WEB_PORT}`（默认 80） | Nginx 托管静态资源，`/api` 反代至后端 |
| 后端接口 | `http://localhost:${BACKEND_PORT}`（默认 8080） | `/api/v1`，健康检查 `/api/v1/common/health` |
| 对象存储控制台 | `http://localhost:${MINIO_CONSOLE_PORT}`（默认 9001） | Silo 控制台；私有桶 `dr-screening` 由后端启动时自动创建 |
| 模型服务 | `http://localhost:${MODEL_PORT}`（默认 8000） | 默认由宿主机提供，见第三节 |

种子账号：`admin / admin123`（管理员）、`doctor / doctor123`（医生），由后端启动时幂等播种。

**停止与清理**：

```bash
docker compose -f deploy/docker-compose.yml down            # 停止（保留数据卷）
docker compose -f deploy/docker-compose.yml down -v         # 停止并删除数据卷（慎用，会清空业务数据）
docker compose -f deploy/docker-compose.yml up -d           # 再次启动（复用镜像与数据卷）
```

---

## 三、模型服务

默认不构建模型镜像，由宿主机常驻：

```bash
cd model-service
py -3.13 -m venv .venv && . .venv\Scripts\Activate.ps1   # 首次
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

> 必须监听 `0.0.0.0`，否则容器内无法通过 `host.docker.internal` 访问。

后端通过 `MODEL_SERVICE_BASE_URL`（默认 `http://host.docker.internal:8000`）访问宿主机模型服务。

**需要完整容器化时**：

```bash
# .env 中设置 MODEL_SERVICE_BASE_URL=http://model-service:8000
docker compose -f deploy/docker-compose.yml --env-file deploy/.env --profile model up -d --build
```

模型镜像默认使用 CPU 版 torch 轮子（`USE_GPU_TORCH=false`，约 1.5–2 GB）；如需 GPU，设 `USE_GPU_TORCH=true`（约 5–7 GB）并配合 NVIDIA Container Toolkit。

---

## 四、关键配置说明

### 1. 镜像源与对象存储镜像

`.env` 中的 `DOCKER_REGISTRY` 控制中间件与基础镜像的拉取源，默认示例为 `docker.m.daocloud.io`（已实测可用）；设为 `docker.io` 即回到官方源。PyPI 依赖由 `PIP_INDEX_URL` 控制，默认清华源。

若加速源不可用，可改用官方源并依赖宿主机代理（Docker Desktop 的代理配置在 Settings → Resources → Proxies）。

### 2. 后端环境变量

后端 `application.yml` 默认连接 `localhost`，**容器网络内必须覆盖为服务名**。这些覆盖已写入 compose：

| 环境变量 | 作用 |
| --- | --- |
| `DB_PASSWORD` / `REDIS_PASSWORD` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 与 `application.yml` 的 `${}` 占位符一一对应 |
| `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 覆盖数据源为容器 `mysql:3306` |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` / `SPRING_DATA_REDIS_PASSWORD` | 覆盖 Redis 为容器 `redis:26379` |
| `DRS_MINIO_ENDPOINT` | 后端读写对象存储的地址，固定 `http://minio:9000` |
| `DRS_MINIO_PUBLIC_ENDPOINT` | **预签名 URL 的对外地址**，只需浏览器可达（默认 `http://localhost:9000`） |
| `DRS_MINIO_REGION` | 对象存储 region，默认 `us-east-1`；显式指定后预签名无需连接服务端 |
| `DRS_MODEL_SERVICE_BASE_URL` | 模型服务地址（默认 `http://host.docker.internal:8000`） |

> `DRS_MINIO_PUBLIC_ENDPOINT` 不可设为 `http://minio:9000`：预签名签名包含 Host，浏览器无法解析 `minio` 主机名。由于后端已显式指定 region，该地址**不需要容器内可达**，仅需浏览器可达。

### 3. 端口约定

全项目 Redis 端口统一为 **26379**。各端口均可在 `.env` 中调整：

| 服务 | 默认宿主机端口 | 容器内端口 | 变量 |
| --- | --- | --- | --- |
| MySQL | 3306 | 3306 | `MYSQL_PORT` |
| Redis | 26379 | 26379 | `REDIS_PORT` |
| 对象存储 API / Console | 9000 / 9001 | 同左 | `MINIO_PORT` / `MINIO_CONSOLE_PORT` |
| 模型服务 / 后端 / 前端 | 8000 / 8080 / 80 | 同左 | `MODEL_PORT` / `BACKEND_PORT` / `WEB_PORT` |

> **端口冲突**：宿主机若已运行 MySQL，`3306` 会被占用，将 `MYSQL_PORT` 改为 `3307` 即可，容器内仍为 3306，不影响服务间通信。

### 4. 数据库初始化

`sql/init/` 已挂载到 MySQL 的 `/docker-entrypoint-initdb.d`，**仅在数据卷首次创建时执行**。脚本幂等，可安全重复执行：

```bash
docker compose -f deploy/docker-compose.yml exec -T mysql \
  mysql -uroot -p"$DB_PASSWORD" dr_screening < sql/init/01_init_dr_screening.sql
```

### 5. 演示数据

```bash
bash tools/seed-demo-data.sh                 # 无影像的演示记录
python tools/gen-demo-data.py               # 用 datasets/ 真实眼底图生成带影像与热力图的记录
```

---

## 五、Nginx 说明

`nginx/nginx.conf` 承担两件事：

1. **静态托管**：`/usr/share/nginx/html` 下的前端构建产物；`/assets/` 长缓存，`/samples/` 缓存 7 天。
2. **接口反代**：`/api/` → `http://backend:8080`，透传 `X-Forwarded-For`（后端操作日志与限流依赖它）。

需要注意的三处：

- `client_max_body_size 100m`：与后端 `max-request-size`（100MB）对齐，支持批量上传；
- `proxy_buffering off`：Excel 导出为流式下载，关闭缓冲避免大文件落盘；
- `try_files $uri $uri/ /index.html`：**必须放在最后**，用于 Vue Router history 模式的路由回退，否则刷新 `/screening/records` 这类子路由会 404。

> 眼底图与热力图存于对象存储私有桶，前端通过**预签名 URL 直连对象存储**，因此默认不在 Nginx 代理。若需隐藏对象存储，可放开配置中的 `/storage/` 段，并同步调整 `DRS_MINIO_PUBLIC_ENDPOINT`。

---

## 六、常见问题

| 现象 | 排查方向 |
| --- | --- |
| 后端容器反复重启 | 看 `logs backend`：多为 `DB_PASSWORD` 未设置或 MySQL 未就绪；compose 已用 `service_healthy` 约束启动顺序 |
| 前端能打开但接口 502 | 后端容器未就绪或崩溃；检查 `logs backend`，确认 8080 已监听 |
| 上传提示“无法连接模型推理服务” | 宿主机模型服务未启动，或 `MODEL_SERVICE_BASE_URL` 未指向 `host.docker.internal:8000` |
| 影像无法显示（403 / 无法解析主机） | `DRS_MINIO_PUBLIC_ENDPOINT` 未设为浏览器可达地址，或预签名 URL 已过期（30 分钟） |
| 登录报错但账号正确 | 数据库未初始化或种子账号未播种；确认数据卷为首次创建（或手工执行 `sql/init`） |
| 登录提示验证码失效 | 验证码存于 Redis 且一次性消费；确认 `SPRING_DATA_REDIS_*` 指向 `redis:26379` |
| 拉取镜像超时 | 将 `DOCKER_REGISTRY` 改为可用的加速源，或为 Docker Desktop 配置代理 |
| `/health` 返回 `trained=false` | 权重文件缺失；确认 `model-service/models/best_model.pth` 存在并已挂载 |
