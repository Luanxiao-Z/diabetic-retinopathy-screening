# 糖尿病视网膜病变（DR）智能筛查系统

面向基层医疗与眼科筛查场景的 **AI 辅助眼底筛查系统**。上传眼底照片后，系统自动完成
糖尿病视网膜病变五级分级（LEVEL_0 正常 ~ LEVEL_4 增殖期），生成 Grad-CAM 可解释热力图，
并给出分级转诊建议；同时提供筛查记录管理、低置信度人工复核、患者纵向随访、统计分析与
诊断报告导出等业务能力。

> **免责声明**：本项目为医学人工智能课程项目，AI 筛查结果**仅供临床参考，不能替代执业医师的诊断意见**。

---

## 一、功能特性

### 筛查业务
- **筛查上传**：选择眼底图后先入队预览（缩略图可点击放大确认），再点击「开始筛查」逐张独立推理；单张失败不影响其它影像并可单独重试，队列实时展示每张状态与耗时
- **筛查记录**：按患者、分级、复核状态与时间检索；支持批量删除与 Excel 导出；表头可拖拽调整列宽并按页记忆
- **随访待办**：汇总待人工复核、需转诊（重度及以上）、逾期未复诊（>90 天且中度以上）三类待办
- **患者随访**：按患者归并历次筛查，识别分级变化方向（进展 / 好转 / 持平 / 首次），提供随访时间线
- **人工复核**：置信度低于阈值（默认 **0.70**）的结果标记为待复核，医师核对后确认复核并留存意见，形成可追溯闭环
- **诊断报告**：A4 版式报告，含受检者信息、眼底原图与热力图、分级结论与各分级概率表、医师复核与免责声明，可打印或另存为 PDF

### 综合看板
- **工作台**：累计筛查、转诊率、需转诊、待人工复核等关键指标，近 30 天趋势（含环比）
- **统计分析**：分级与转诊建议分布、近 30 天趋势与环比
- **模型信息**：模型版本、骨干、推理设备与训练指标（准确率、宏平均 F1、逐类 F1、逐轮训练曲线）

### 系统管理
- **用户管理**：账号分页查询、新增、编辑、启停与逻辑删除
- **字典管理**：字典域与其下字典项的维护（增删改、排序、级联删除）
- **操作日志**：登录、筛查上传/删除/导出、人工复核、账号变更等关键动作留痕，含操作人、结果、耗时与 IP

### 通用能力
- **权限体系**：轻量化角色映射（ADMIN / DOCTOR）+ 数据权限（ALL / SELF），不建 RBAC 表
- **多标签页**：页签切换保留页面状态，支持关闭单个 / 其他 / 全部，刷新后不丢失
- **使用指南与关于系统**：面向使用者的操作说明、FAQ、技术架构与第三方组件说明

---

## 二、技术栈

| 层次 | 技术选型 |
| --- | --- |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router + ECharts（包管理器 Yarn） |
| 后端 | Spring Boot 3.4（Java 21）+ Maven 多模块 + MyBatis-Plus + Redis（Token 会话）+ Apache POI |
| 模型服务 | Python 3.12 + FastAPI + PyTorch（CUDA cu130）+ torchvision，自实现 Grad-CAM |
| 数据存储 | MySQL 8（业务数据）、Redis 7（会话缓存）、MinIO（眼底图与热力图私有桶） |

---

## 三、系统架构

```
┌──────────────┐   HTTP    ┌──────────────────┐   HTTP    ┌────────────────────┐
│  前端 Web     │ ────────► │  后端 Backend     │ ────────► │  模型服务           │
│  Vue3 + Vite │  /api/v1  │  Spring Boot 3    │  /predict │  FastAPI + PyTorch │
│  端口 5173    │ ◄──────── │  端口 8080        │ ◄──────── │  端口 8000          │
└──────────────┘           └────────┬─────────┘           └────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              ┌──────────┐   ┌──────────┐   ┌──────────────┐
              │ MySQL 8  │   │ Redis 7  │   │ MinIO        │
              │ 业务数据  │   │ 会话缓存  │   │ 影像私有桶    │
              │ 3306     │   │ 6379     │   │ 9000 / 9001  │
              └──────────┘   └──────────┘   └──────────────┘
```

**关键约定**

- 响应体统一为 `{ code, msg, data }`；成功 `code=200`，业务异常保持 HTTP 200 + 信封 code（401/403 同时返回对应 HTTP 状态码）
- 认证采用 **UUID 令牌 + Redis 会话**（非 JWT），令牌通过请求头 `X-Access-Token` 传递
- 影像存于 MinIO **私有桶**，前端通过 **30 分钟有效期的预签名 URL** 访问
- 后端与模型服务之间为 HTTP 调用，模型服务不可达时后端降级返回 503

---

## 四、目录结构

```
diabetic-retinopathy-screening/
├── backend/                     Maven 聚合工程（Java 21）
│   ├── drs-dependencies/        依赖版本管理 BOM
│   ├── drs-module/
│   │   ├── drs-module-common/   通用：统一响应、异常、审计契约、密码工具
│   │   ├── drs-module-security/ 安全：认证上下文、权限注解与解析
│   │   ├── drs-module-system/   系统：认证、用户、字典、操作日志
│   │   └── drs-module-screening/筛查：上传推理、记录、统计、导出、模型信息
│   └── drs-app/drs-app-server/  启动模块（fat-jar）
├── frontend/web/                Vue3 + Vite PC 端
│   └── src/
│       ├── api/                 接口封装
│       ├── components/          通用组件（AppIcon/PageHeader/StatCard/EChart/ResultCard）
│       ├── composables/         组合式函数（表格列宽记忆等）
│       ├── layouts/             布局外壳（顶栏 + 分组侧栏 + 多标签页）
│       ├── styles/              设计令牌与主题
│       ├── types/               类型定义
│       └── views/               页面
├── model-service/               FastAPI 模型服务
│   ├── app/                     推理管线、Grad-CAM、模型加载、配置
│   └── training/                训练数据加载与训练入口
├── datasets/                    APTOS 2019 数据集（不入库）
├── deploy/                      Nginx 与 Docker 容器化部署示例（见 deploy/README.md）
├── docs/                        项目文档
├── spec/                        规范体系（效力高于 docs 与 README）
├── sql/init/                    MySQL 建表与字典初始化脚本（幂等）
└── tools/                       辅助脚本（Maven 包装、演示数据生成）
```

---

## 五、环境要求

| 组件 | 版本要求 |
| --- | --- |
| JDK | 21 |
| Maven | 3.9.x |
| Node.js | 18+（推荐 22） |
| Yarn | 1.22 |
| Python | 3.12+ |
| MySQL / Redis / MinIO | 8.0 / 7 / 最新版（本地或容器均可） |
| NVIDIA 驱动 + CUDA | 可选；模型服务支持 CPU 回退 |

---

## 六、快速开始

### 1. 准备基础设施

项目默认以**本地运行**方式交付，需自备 MySQL、Redis、MinIO 三个服务。若希望一键起停，可直接使用容器编排（见 [deploy/README.md](deploy/README.md)）。

连接信息通过环境变量注入，**请勿将明文写入版本库**：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | MySQL 密码（业务库 `dr_screening`，以 `root` 连接） |
| `REDIS_PASSWORD` | Redis 密码 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 凭据（后端启动时自动创建私有桶 `dr-screening`） |

数据库初始化：首次部署时执行 `sql/init/01_init_dr_screening.sql`（脚本幂等，可重复执行）。

### 2. 启动模型服务

```bash
cd model-service
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000

# 健康检查
curl http://localhost:8000/health
```

> `requirements.txt` 默认拉取 CUDA 版 PyTorch；无 NVIDIA GPU 时请将末两行改为 `+cpu` 轮子。

### 3. 启动后端

```bash
bash tools/mvn.sh clean install -DskipTests          # 构建 fat-jar
cd backend/drs-app/drs-app-server/target
java -jar drs-app-server-1.0.0.jar --server.port=8080
```

首次启动会自动完成：连接数据库、创建 MinIO 桶、幂等播种种子账号。

### 4. 启动前端

```bash
cd frontend/web
yarn install
yarn dev        # 开发服务器 http://localhost:5173，/api 代理至后端 8080
yarn build      # 生产构建，产物位于 dist/
```

### 5. 默认账号

| 账号 | 密码 | 角色 | 数据权限 |
| --- | --- | --- | --- |
| `admin` | `admin123` | 管理员 | 全部数据（ALL） |
| `doctor` | `doctor123` | 医生 | 仅本人数据（SELF） |

> 账号由后端启动时幂等播种，已存在则跳过。

---

## 七、默认端口

| 服务 | 端口 |
| --- | --- |
| 前端 web（dev） | 5173 |
| 后端 backend | 8080 |
| 模型服务 model-service | 8000 |
| MySQL | 3306 |
| Redis | 6379 |
| MinIO API / Console | 9000 / 9001 |

---

## 八、接口概览

所有业务接口基础路径为 `/api/v1`，鉴权通过请求头 `X-Access-Token`。

### 认证与公共

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/sessions` | 登录，返回令牌、角色、权限集与数据权限 |
| DELETE | `/auth/sessions` | 登出（令牌失效） |
| GET | `/common/users/me` | 当前用户档案 |
| GET | `/common/users/me/permissions` | 当前用户权限集 |
| PUT | `/common/users/me/password` | 修改当前账号密码 |
| GET | `/common/dicts`、`/common/dicts/{code}/items` | 字典查询 |

### 筛查业务（`biz:screening:*`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/biz/screening-records` | 上传眼底图并筛查（multipart） |
| GET | `/biz/screening-records` | 分页查询（受数据权限约束） |
| GET | `/biz/screening-records/{id}` | 详情（含预签名 URL） |
| DELETE | `/biz/screening-records/{id}` | 删除（逻辑删除并清理对象存储） |
| PATCH | `/biz/screening-records/{id}/review` | 人工复核确认 |
| GET | `/biz/screening-records/statistics` | 统计（分布、转诊率、趋势、环比） |
| GET | `/biz/screening-records/patients` | 患者随访聚合 |
| GET | `/biz/screening-records/exports` | 导出 Excel |
| GET | `/biz/screening-records/model-info` | 模型元信息与训练指标 |

### 系统管理（`admin:*`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/admin/users`、`/admin/users/{id}` | 用户查询与维护 |
| PATCH | `/admin/users/{id}/state` | 启用 / 停用账号 |
| GET/POST/PUT/DELETE | `/admin/dict-domains`、`/admin/dict-domains/{code}/items` | 字典域与字典项维护 |
| GET | `/admin/operation-logs` | 操作日志分页查询 |

### 模型服务（独立部署，不走 `/api/v1`）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/health` | 健康检查（设备、是否已加载训练权重） |
| GET | `/model/info` | 模型元信息与训练指标 |
| POST | `/predict` | 多分类推理 |
| POST | `/cam` | 生成 Grad-CAM 热力图（PNG） |

---

## 九、权限模型

采用**轻量化角色映射 + 数据权限**，不建 RBAC 表：

| 角色 | 数据权限 | 功能权限 |
| --- | --- | --- |
| ADMIN | ALL（全量数据） | `admin:*`、`common:dict:view`、`biz:screening:*` |
| DOCTOR | SELF（仅本人记录） | `common:dict:view`、`biz:screening:create/view/export/delete/review` |

数据权限在服务层依据登录主体拼接过滤条件；功能权限由 `@RequirePermission` 注解与前端
`v-permission` 指令双向校验，**前后端权限编码保持一致**。

---

## 十、数据模型

| 表 | 说明 |
| --- | --- |
| `sys_user` | 系统用户（含角色、状态、密码摘要） |
| `sys_dict_domain`、`sys_dict_item` | 字典域与字典项 |
| `biz_screening_record` | 筛查记录（分级、置信度、各分级概率、转诊建议、复核信息、对象键） |
| `sys_operation_log` | 操作审计日志 |

建表与字典初始化脚本：`sql/init/01_init_dr_screening.sql`（幂等，含增量迁移判断）。

---

## 十一、模型说明

| 项 | 说明 |
| --- | --- |
| 骨干网络 | MobileNetV3-Small（5 类分类头） |
| 训练数据 | APTOS 2019（sovitrath 224×224 预处理版，3662 张，5 类） |
| 训练入口 | `model-service/training/train.py`（分层抽样、类别加权、按验证集宏 F1 选优） |
| 权重路径 | `model-service/models/best_model.pth`（**不入版本库**；缺失时回退随机初始化） |
| 分级映射 | `LEVEL_0/1 → REVIEW`、`LEVEL_2 → CLINIC`、`LEVEL_3/4 → REFERRAL` |

```bash
cd model-service
python -m training.train --data-root ../datasets/aptos2019_224x224 \
  --epochs 30 --batch-size 32 --lr 3e-4
```

训练完成后导出 `models/best_model.pth` 与 `models/train_metrics.json`，推理服务**零代码切换**。
模型性能指标可在系统内「综合看板 → 模型信息」页查看。

---

## 十二、开发规范与质量

- **规范效力**：`spec/` 目录效力高于 `docs/` 与本文档，冲突时以 `spec/` 为准
- **前端**：TypeScript 全量，ESLint + Prettier + Stylelint

  ```bash
  cd frontend/web
  yarn build      # vue-tsc --noEmit && vite build
  yarn lint       # eslint
  yarn stylelint  # stylelint
  ```

- **后端**：统一响应体、统一异常处理、接口 100% 提供 Knife4j 中文注解
- **提交信息**：遵循 Conventional Commits（`feat` / `fix` / `docs` / `refactor` / `perf` / `chore` 等）

---

## 十三、部署

- **本地运行**：见「六、快速开始」，三个服务分别启动，基础设施由本机提供
- **容器化**：`deploy/` 提供 `docker-compose.yml`、三个 Dockerfile 与 Nginx 配置，
  一键启动全部六个服务（MySQL / Redis / MinIO / 模型服务 / 后端 / 前端），
  详见 [deploy/README.md](deploy/README.md)

---

## 十四、辅助脚本

| 脚本 | 用途 |
| --- | --- |
| `tools/mvn.sh` | Git Bash 下 Maven 包装脚本（解决 POSIX 路径问题） |
| `tools/seed-demo-data.sh` | 生成演示数据（账号 + 筛查记录），幂等，支持 `--clean` |

---

## 十五、许可证

本项目采用 [MIT License](LICENSE)。第三方组件遵循各自开源许可，清单见系统内「帮助 → 关于系统」页。
