# 部署说明

本目录提供 **Docker Compose 一键编排**，用于在未安装 MySQL / Redis / MinIO 等中间件的环境里快速启动完整系统。编排包含 6 个服务：基础设施 3 个（MySQL、Redis、MinIO）+ 应用 3 个（模型服务、后端、前端），并附带各自的 Dockerfile 与 Nginx 配置。

若已自行部署中间件、只需分别启动三个应用，见仓库根 [README.md 第六节](../README.md#六快速开始)。

> **前提**：仅需安装 Docker Engine 与 Docker Compose 插件；JDK、Maven、Node、Python 均由镜像内构建阶段提供，宿主机无需安装。模型权重 `model-service/models/best_model.pth` 与 `train_metrics.json` **已纳入版本控制**，克隆仓库后即可直接使用，无需额外下载或训练。

---

## 一、目录结构

| 文件 | 用途 |
| --- | --- |
| `docker-compose.yml` | 6 个服务的编排、健康检查、依赖顺序、数据卷与网络 |
| `.env.example` | 环境变量模板（复制为 `.env` 后填写；`.env` 不入库） |
| `docker/Dockerfile.backend` | 后端多阶段构建：Maven 3.9 + JDK 21 编译 → JRE 运行 fat-jar |
| `docker/Dockerfile.frontend` | 前端多阶段构建：Node 22 + Yarn 打包 → Nginx 托管 `dist/` |
| `docker/Dockerfile.model` | 模型服务：Python + FastAPI + PyTorch，Uvicorn 启动 |
| `nginx/nginx.conf` | Nginx 配置：SPA 路由回退、`/api` 反代、上传体积限制 |

三个 Dockerfile 的构建上下文均为**仓库根目录**（compose 中的 `context: ..`），因此必须从仓库根目录执行 compose 命令。

---

## 二、一键启动与停止

```bash
# 1) 准备环境变量（务必修改密码，勿使用示例值）
cp deploy/.env.example deploy/.env
$EDITOR deploy/.env

# 2) 构建并后台启动全部服务
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build

# 3) 查看状态与日志
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f backend
```

首次构建需拉取基础镜像并编译前后端，耗时较长（模型服务还需下载约 3 GB 的 PyTorch 轮子）；后续启动复用镜像缓存。

启动完成后：

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 系统前端 | `http://<主机>:${WEB_PORT}`（默认 80） | Nginx 托管静态资源，`/api` 反代至后端 |
| 后端接口 | `http://<主机>:${BACKEND_PORT}`（默认 8080） | `/api/v1`，健康检查 `/api/v1/common/health` |
| 模型服务 | `http://<主机>:${MODEL_PORT}`（默认 8000） | `/health`、`/predict`、`/cam`、`/model/info` |
| MinIO 控制台 | `http://<主机>:${MINIO_CONSOLE_PORT}`（默认 9001） | 私有桶 `dr-screening` 由后端启动时自动创建 |

种子账号：`admin / admin123`（管理员）、`doctor / doctor123`（医生），由后端启动时幂等播种。

**停止与清理**：

```bash
docker compose -f deploy/docker-compose.yml down            # 停止（保留数据卷）
docker compose -f deploy/docker-compose.yml down -v         # 停止并删除数据卷（慎用，会清空业务数据）
docker compose -f deploy/docker-compose.yml up -d           # 再次启动（复用镜像与数据卷）
```

---

## 三、关键配置说明

### 1. 后端环境变量

后端 `application.yml` 默认连接 `localhost`，**在容器网络中必须覆盖为服务名**，否则会连接失败。这些覆盖已写入 compose，无需手工添加：

| 环境变量 | 作用 |
| --- | --- |
| `DB_PASSWORD` / `REDIS_PASSWORD` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 与 `application.yml` 的 `${}` 占位符一一对应 |
| `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 覆盖数据源为容器 `mysql:3306` |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` / `SPRING_DATA_REDIS_PASSWORD` | 覆盖 Redis 为容器 `redis:26379` |
| `DRS_MODEL_SERVICE_BASE_URL` | 覆盖模型服务地址为 `http://model-service:8000` |

### 2. 端口约定

全项目 Redis 端口统一为 **26379**：物理机部署（`application.yml`）、容器编排（Redis 服务监听 26379、宿主机映射 26379）、以及各工具脚本均一致，避免出现两套口径。

| 服务 | 宿主机（可改） | 容器内 | 环境变量 |
| --- | --- | --- | --- |
| MySQL | 3306 | 3306 | `MYSQL_PORT` |
| Redis | 26379 | 26379 | `REDIS_PORT` |
| MinIO API / Console | 9000 / 9001 | 9000 / 9001 | `MINIO_PORT` / `MINIO_CONSOLE_PORT` |
| 模型服务 / 后端 / 前端 | 8000 / 8080 / 80 | 同左 | `MODEL_PORT` / `BACKEND_PORT` / `WEB_PORT` |

> Redis 端口由 `.env` 的 `REDIS_PORT` 统一驱动：容器监听端口、宿主机映射、健康检查与后端 `SPRING_DATA_REDIS_PORT` 均引用该变量，**如需改端口只需改这一处**。

### 3. 模型权重

`model-service/models/best_model.pth`（约 5.9 MB）与 `train_metrics.json` 已随仓库分发。compose 将宿主机 `model-service/models` 目录挂载到容器 `/app/models`，因此：

- 克隆仓库后**开箱即用**，`/health` 应返回 `trained=true`；
- 本地重训后替换该文件即可生效，无需重建镜像；
- 若文件缺失，服务仍可启动但回退为随机初始化（`trained=false`），**仅可用于接口调试，不具备真实筛查能力**。

### 4. GPU 与 CPU

`Dockerfile.model` 按 `requirements.txt` 默认拉取 **CUDA(cu130)** 版 PyTorch。容器默认无 GPU 运行时，会回退到 CPU 推理（可正常运行，速度较慢）：

- **纯 CPU 部署**：把 `model-service/requirements.txt` 末两行改为 `+cpu` 轮子后重新构建，可显著减小镜像体积；
- **GPU 部署**：需安装 NVIDIA Container Toolkit，并在 compose 中为 `model-service` 声明 `runtime: nvidia` 或 `deploy.resources.reservations.devices`。

### 5. 数据库初始化

`sql/init/` 已挂载到 MySQL 的 `/docker-entrypoint-initdb.d`，**仅在数据卷首次创建时执行**。脚本为幂等设计（`CREATE TABLE IF NOT EXISTS` + `INSERT ... ON DUPLICATE KEY UPDATE`），可安全重复执行：

```bash
docker compose -f deploy/docker-compose.yml exec -T mysql \
  mysql -uroot -p"$DB_PASSWORD" dr_screening < sql/init/01_init_dr_screening.sql
```

### 6. 演示数据

需要演示数据时可执行仓库脚本（连接信息取环境变量，幂等）：

```bash
bash tools/seed-demo-data.sh                 # 无影像的演示记录
python tools/gen-demo-data.py               # 用 datasets/ 真实眼底图生成带影像与热力图的记录
```

---

## 四、Nginx 说明

`nginx/nginx.conf` 承担两件事：

1. **静态托管**：`/usr/share/nginx/html` 下的前端构建产物；`/assets/` 长缓存，`/samples/` 缓存 7 天。
2. **接口反代**：`/api/` → `http://backend:8080`，透传 `X-Forwarded-For`（后端操作日志与限流依赖它）。

需要注意的三处：

- `client_max_body_size 25m`：与后端 multipart 上限（20MB）对齐，眼底图上传需放宽；
- `proxy_buffering off`：Excel 导出为流式下载，关闭缓冲避免大文件落盘；
- `try_files $uri $uri/ /index.html`：**必须放在最后**，用于 Vue Router history 模式的路由回退，否则刷新 `/screening/records` 这类子路由会 404。

> 眼底图与热力图存于 MinIO 私有桶，前端通过**预签名 URL 直连 MinIO**，因此默认不在 Nginx 代理对象存储。若需隐藏 MinIO，可放开配置中的 `/storage/` 段，并同步调整后端生成预签名地址的逻辑。

---

## 五、常见问题

| 现象 | 排查方向 |
| --- | --- |
| 后端容器反复重启 | 看 `logs backend`：多为 `DB_PASSWORD` 未设置或 MySQL 未就绪；compose 已用 `service_healthy` 约束启动顺序 |
| 前端能打开但接口 502 | 后端容器未就绪或崩溃；检查 `logs backend`，确认 8080 已监听 |
| 上传提示“无法连接模型推理服务” | 模型服务未启动或 `DRS_MODEL_SERVICE_BASE_URL` 未指向 `model-service:8000` |
| 登录报错但账号正确 | 数据库未初始化或种子账号未播种；确认数据卷为首次创建（或手工执行 `sql/init`） |
| 登录提示验证码失效 | 验证码存于 Redis 且一次性消费；确认 `SPRING_DATA_REDIS_*` 指向 `redis:26379` |
| 影像无法显示 | 确认 MinIO 已就绪且后端已自动创建 `dr-screening` 桶；预签名 URL 30 分钟过期 |
| `/health` 返回 `trained=false` | 权重文件缺失；确认 `model-service/models/best_model.pth` 存在并已挂载 |
