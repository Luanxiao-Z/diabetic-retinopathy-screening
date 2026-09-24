# 部署说明（容器化示例）

本目录提供 **Docker 容器化部署示例**。项目默认交付形态为**本地直接运行**（见仓库根 `README.md`），
本目录配置用于需要一键起停全部依赖与服务的场景，或作为课程交付的部署材料。

---

## 一、目录结构

```
deploy/
├── docker-compose.yml         # 六个服务的编排（MySQL / Redis / MinIO / 模型服务 / 后端 / 前端）
├── .env.example               # 环境变量模板（复制为 .env 后填写，.env 不入库）
├── docker/
│   ├── Dockerfile.backend     # 后端：Maven 构建 → JRE 运行（多阶段）
│   ├── Dockerfile.frontend    # 前端：Node 构建 → Nginx 托管（多阶段）
│   └── Dockerfile.model       # 模型服务：Python + FastAPI + PyTorch
└── nginx/
    └── nginx.conf             # Nginx 配置（SPA 回退 + /api 反代 + 上传体积）
```

---

## 二、快速开始

```bash
# 1) 准备环境变量（务必修改密码，勿使用示例值）
cp deploy/.env.example deploy/.env
vi deploy/.env

# 2) 构建并启动全部服务
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build

# 3) 查看状态与日志
docker compose -f deploy/docker-compose.yml ps
docker compose -f deploy/docker-compose.yml logs -f backend
```

启动完成后：

| 入口 | 地址 | 说明 |
| --- | --- | --- |
| 系统前端 | `http://<主机>:${WEB_PORT}`（默认 80） | Nginx 托管，`/api` 反代至后端 |
| 后端接口 | `http://<主机>:${BACKEND_PORT}`（默认 8080） | `/api/v1` |
| 模型服务 | `http://<主机>:${MODEL_PORT}`（默认 8000） | `/health`、`/predict`、`/cam`、`/model/info` |
| MinIO 控制台 | `http://<主机>:${MINIO_CONSOLE_PORT}`（默认 9001） | 桶 `dr-screening` 由后端启动时自动创建 |

种子账号：`admin / admin123`（管理员）、`doctor / doctor123`（医生），由后端启动时幂等播种。

停止与清理：

```bash
docker compose -f deploy/docker-compose.yml down            # 停止（保留数据卷）
docker compose -f deploy/docker-compose.yml down -v         # 停止并删除数据卷（慎用）
```

---

## 三、关键配置说明

### 1. 后端环境变量（易错点）

后端 `application.yml` 读取的是**固定名称**的环境变量，且数据源/Redis/模型服务地址默认指向 `127.0.0.1`，
**在容器中必须覆盖为容器内服务名**，否则会连接失败：

| 环境变量 | 作用 |
| --- | --- |
| `DB_PASSWORD` / `REDIS_PASSWORD` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | 与 `application.yml` 的 `${}` 占位符一一对应 |
| `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | 覆盖数据源连接 |
| `SPRING_DATA_REDIS_HOST` / `SPRING_DATA_REDIS_PORT` / `SPRING_DATA_REDIS_PASSWORD` | 覆盖 Redis 连接 |
| `DRS_MODEL_SERVICE_BASE_URL` | 覆盖模型服务地址为 `http://model-service:8000` |

> 这些均已写入 `docker-compose.yml`，无需手工添加。

### 2. 模型权重

训练权重 `model-service/models/best_model.pth` **不入镜像也不入库**（体积与许可考虑），
由 compose 挂载宿主机 `model-service/models` 目录提供。
若该目录为空，模型服务仍可启动，但会**回退为随机初始化权重**（`/health` 的 `trained=false`），
仅适合验证链路，不具备真实筛查能力。获取权重的方式：

```bash
# 本地训练（推荐，约 3 分钟）
cd model-service && python -m training.train --data-root ../datasets/aptos2019_224x224
```

### 3. GPU 与 CPU

`Dockerfile.model` 默认按 `requirements.txt` 拉取 **CUDA(cu130)** 版 PyTorch。容器默认无 GPU 运行时：

- **纯 CPU 部署**：把 `model-service/requirements.txt` 末两行改为 `+cpu` 轮子后重新构建；
- **GPU 部署**：需安装 NVIDIA Container Toolkit，并在 compose 中为 `model-service` 声明
  `runtime: nvidia` 或 `deploy.resources.reservations.devices`。

### 4. 数据库初始化

`sql/init/` 已挂载到 MySQL 的 `/docker-entrypoint-initdb.d`，**仅在数据卷首次创建时执行**。
脚本为幂等设计（`CREATE TABLE IF NOT EXISTS` + 增量迁移判断），可安全重复执行：

```bash
docker compose -f deploy/docker-compose.yml exec -T mysql \
  mysql -uroot -p"$DB_PASSWORD" dr_screening < sql/init/01_init_dr_screening.sql
```

### 5. 演示数据

需要演示数据时可执行仓库脚本（连接信息取环境变量，幂等）：

```bash
DB_HOST=127.0.0.1 DB_PORT=${MYSQL_PORT} DB_PASSWORD=****** \
  bash tools/seed-demo-data.sh
```

---

## 四、Nginx 说明

`nginx/nginx.conf` 承担两件事：

1. **静态托管**：`/usr/share/nginx/html` 下的前端构建产物；`/assets/` 长缓存，`/samples/` 缓存 7 天。
2. **接口反代**：`/api/` → `http://backend:8080`，透传 `X-Forwarded-For`（后端操作日志记录客户端 IP 依赖它）。

需要注意的三处：

- `client_max_body_size 25m`：与后端 multipart 上限（20MB）对齐，眼底图上传需放宽；
- `proxy_buffering off`：Excel 导出为流式下载，关闭缓冲避免大文件落盘；
- `try_files $uri $uri/ /index.html`：**必须放在最后**，用于 Vue Router history 模式的路由回退，
  否则刷新 `/screening/records` 这类子路由会 404。

> 眼底图与热力图存于 MinIO 私有桶，前端通过**预签名 URL 直连 MinIO**，
> 因此默认不在 Nginx 代理对象存储。若需内网隐藏 MinIO，可放开配置中的 `/storage/` 段，
> 并同步调整后端生成预签名地址的逻辑。

---

## 五、常见问题

| 现象 | 排查方向 |
| --- | --- |
| 后端容器反复重启 | 看 `logs backend`：多为 `DB_PASSWORD` 未设置或 MySQL 未就绪；compose 已用 `service_healthy` 约束启动顺序 |
| 前端能打开但接口 502 | 后端容器未就绪或崩溃；检查 `logs backend`，确认 8080 已监听 |
| 上传提示「无法连接模型推理服务」 | 模型服务未启动或 `DRS_MODEL_SERVICE_BASE_URL` 未指向 `model-service:8000` |
| 登录报错但账号正确 | 数据库未初始化或种子账号未播种；确认数据卷为首次创建（或手工执行 `sql/init`） |
| 影像无法显示 | 权重/桶异常：确认 MinIO 已就绪且后端已自动创建 `dr-screening` 桶；预签名 URL 30 分钟过期 |
