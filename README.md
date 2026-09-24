# 糖尿病视网膜病变（DR）智能筛查系统

基于 `spec/` 规范体系与 `docs/项目开发计划.md`（v1.1 决策冻结版）实现的工程骨架。**阶段 0（脚手架）、阶段 1（数据层 + 认证接口）、阶段 2（认证与权限）、阶段 3（模型服务）与阶段 4（业务核心）已完成**：后端多模块编译/构建通过、前端构建通过、MySQL 建表与字典初始化已落库、登录 / 字典 / 当前用户接口端到端验证可用；阶段 2 新增管理员用户与字典 CRUD 接口并接入 `@RequirePermission`，角色—权限映射与数据权限 SELF/ALL 落地，401/403 鉴权端到端验证通过；阶段 3 实现 FastAPI 模型服务（MobileNetV3-Small 5 类推理管线、自实现 Grad-CAM 热力图、/predict 与 /cam 接口，CUDA cu130 版 PyTorch 跑在本地 RTX 4050 上）。**模型训练已完成**：基于 APTOS 2019（3662 张）训练 MobileNetV3-Small（5 类），权重导出至 `model-service/models/best_model.pth`，推理服务零代码切换（权重缺失时仍自动回退随机初始化，仅用于管线联调）。测试集准确率 **81.47%**、宏平均 F1 **0.6555**（详见「模型训练」章节）。

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

> 模型已基于 APTOS 2019 完成训练，权重位于 `models/best_model.pth`（缺失或损坏时自动回退随机初始化，仅用于管线联调）。训练与评测细节见下文「模型训练」章节。

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
  "model_version": "aptos2019-mobilenetv3s-1.0.0"
}
```

| 方法 | 路径 | 说明 | 请求 |
| --- | --- | --- | --- |
| GET | `/health` | 健康检查（Docker 探针）；返回 `status / model / device / cuda_available / trained` | — |
| POST | `/predict` | 多分类推理：返回分级编码、标签、置信度、各分级概率、转诊建议 | `multipart/form-data: image=眼底图` |
| POST | `/cam` | 生成 Grad-CAM 热力图，返回叠加 PNG（同时以响应头返回 `X-Record-Id` / `X-Target-Level` / `X-Confidence`） | `multipart/form-data: image=眼底图` |

> 分级与转诊映射（与后端 `B_DR_LEVEL` / `B_DR_SUGGESTION` 一致，业务层维护）：`LEVEL_0/1 → REVIEW`（定期复查）、`LEVEL_2 → CLINIC`（建议眼科就诊）、`LEVEL_3/4 → REFERRAL`（建议尽快转诊）。阶段 4 已实现热力图上传统 MinIO 并落库 `grad_cam_key`。
> `/health` 的 `trained` 字段由权重是否成功加载决定；`model` 字段在加载成功时为 `aptos2019-mobilenetv3s-1.0.0`，回退随机初始化时为 `dev-untrained-0.1.0`。

## 模型训练（APTOS 2019，已完成）

**数据集**：`datasets/aptos2019_224x224/`（sovitrath 224×224 预处理版 APTOS 2019，3662 张 5 类，已 gitignore）。分层抽样 8:1:1 → train 2929 / val 366 / test 367。

**训练配置**：MobileNetV3-Small（ImageNet 预训练初始化）+ 5 类头；加权 CrossEntropyLoss（逆频率，缓解类别不均衡）；AdamW（lr 3e-4、weight-decay 1e-4）+ CosineAnnealingLR；batch 32、60 epochs；按**验证集宏平均 F1** 选优保存。

**复现命令**（在 `model-service` 目录下）：

```bash
python -m training.train --data-root ../datasets/aptos2019_224x224 \
  --epochs 60 --batch-size 32 --lr 3e-4
# 离线（无法下载 ImageNet 预训练权重）时追加 --no-pretrained
```

**测试集结果**（367 张）：

| 指标 | 数值 |
| --- | --- |
| 准确率 | 0.8147 |
| 宏平均 F1 | 0.6555 |
| 最佳验证集宏 F1 | 0.6940（第 29 轮） |
| 各级 F1 | LEVEL_0 **0.978** / LEVEL_1 0.617 / LEVEL_2 0.759 / LEVEL_3 **0.378** / LEVEL_4 0.546 |
| 测试集样本数 | 181 / 37 / 100 / 20 / 29 |

> **结论与局限**：`LEVEL_0`（正常）识别可靠，`LEVEL_3`（重度）F1 仅 0.378——测试集中该类仅 20 张，样本量过小导致指标不稳定；`LEVEL_1` 与 `LEVEL_4` 亦有明显混淆。60 轮训练中最佳验证轮次出现在第 **29** 轮，此后验证指标下降（末轮 train acc 0.977 vs val acc 0.798，存在过拟合），说明脚本原有 `--epochs 30` 默认值已接近最优，继续增加轮次收益有限。进一步提升方向：更大规模数据 / 更强增强 / 类别重采样。
> 训练指标完整记录：`model-service/models/train_metrics.json`（含 60 轮 history）。
> **权重与训练指标均已纳入版本控制**（体积约 5.9MB / 23KB），克隆仓库后即可直接使用，无需重新训练；仅训练数据集 `datasets/` 与生成的热力图不入库。

**训练性能**：Windows 下 DataLoader 每 epoch 重建 worker 会重新 `import torch`（单次约 13.7s）。已默认启用 `persistent_workers=True`、`test_loader` 用 `num_workers=0`，稳态由 32.61s/epoch 降至 **4.25s/epoch**（约 7.7 倍），60 轮总耗时约 5 分钟。

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

## 前端界面与设计系统（2026-09-24 重构）

前端 PC 端界面已按「医疗可及性优先」原则重构，布局语言参考政务侧工作台原型（顶栏 + 分组侧栏 + 面包屑 + 页头 + 卡片）。

**设计令牌**（`frontend/web/src/styles/theme.css`）：医疗青品牌色阶（`--drs-primary` #0891B2）、中性墨色阶（正文对比度 ≥ 4.5:1）、语义色、圆角 6/8/12/16、分层阴影、间距节奏，并对齐 Element Plus 主题变量。

**布局结构**：

| 区域 | 说明 |
| --- | --- |
| 顶栏 | 导航栏三态切换（展开 / 图标 / 隐藏，持久化并按视口宽度自动降级）、当前页标题、**数据范围徽标**（全部数据 / 仅本人数据）、账号菜单 |
| 侧栏 | 按模块分组可折叠（综合看板 / 筛查业务 / 账户），按权限过滤，选中态高亮 |
| 内容区 | 面包屑 + 页头（标题 / 副标题 / 操作区）+ 卡片化内容 |

**通用组件**：`AppIcon`（24×24 线性图标集，全站统一，不使用 emoji 作图标）、`PageHeader`、`StatCard`（可点击指标卡，原生 button 语义 + 键盘可达）、`EChart`（支持高度档位）、`ResultCard`。

**无障碍与交互**：`:focus-visible` 焦点环、`prefers-reduced-motion` 降级、可点击元素均有 `cursor: pointer` 与悬停反馈、hover 不产生位移、375 / 768 / 1024 / 1440 四档响应式（375px 下无水平滚动）。

**医疗场景细节**：筛查结果卡标注「AI 辅助筛查结果，仅供临床参考，最终诊断请以眼科医师意见为准」；分级与转诊建议采用浅底深字的软色标签，避免大面积实色；置信度以「细进度条 + 数值」呈现便于横向比较。

> 校验：`vue-tsc --noEmit` 0 错误、`vite build` 通过、`eslint src` 0 问题；另用真实 Chrome 对 7 个页面 + 侧栏三态 + 详情弹窗 + 两档窄屏做截图核验。

## 功能拓展（2026-09-24）

在既有「上传→推理→落库→查询→统计→导出」闭环之上，补充了 5 项功能。其中**系统管理是此前的明显缺口**：后端 `admin:user:*` / `admin:dict:*` 权限与接口齐备，但前端没有任何入口，导致这些权限实际不可用。

| 功能 | 说明 | 位置 |
| --- | --- | --- |
| **用户管理** | 账号分页（用户名模糊 / 角色 / 状态）、新增、编辑、启停、逻辑删除；禁止操作当前登录账号 | 系统管理 → 用户管理 |
| **字典管理** | 字典域列表与增删改，右侧维护该域下字典项（含排序）；删除域级联删除其下项 | 系统管理 → 字典管理 |
| **操作日志** | 关键动作留痕（登录、筛查上传/删除/导出、用户与字典变更），记录操作人、模块、对象、结果、耗时与 IP | 系统管理 → 操作日志 |
| **低置信度人工复核** | 置信度低于阈值（默认 **0.70**）的筛查结果标记为「待复核」，支持按复核状态筛选；看板与统计给出待复核数量 | 筛查记录 / 筛查看板 |
| **患者随访** | 按患者归并历次筛查，识别分级变化方向（**进展 / 好转 / 持平 / 首次**），并提供随访时间线抽屉 | 筛查业务 → 患者随访 |

**新增/扩展的后端接口**：

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/biz/screening-records/patients` | 患者随访分页（按患者聚合，含分级变化方向与复核需求） | `biz:screening:view` |
| GET | `/admin/operation-logs` | 操作日志分页（操作人 / 模块 / 结果 / 时间范围） | `admin:log:view`（新增） |

> 列表查询新增 `needReview`（是否只看待复核）与 `exactPatientName`（随访时间线精确匹配）两个参数；记录 VO 与统计 VO 增加 `needReview` / `reviewThreshold` / `needReviewCount` 字段。
> **架构约束**：`drs-module-screening` 不依赖 `drs-module-system`，故审计记录契约 `OperationLogRecorder` 下沉至 `drs-module-common`，由 system 模块提供实现，避免反向依赖。

**新增数据表**：`sys_operation_log`（操作审计日志），建表语句已并入 `sql/init/01_init_dr_screening.sql`（幂等，`IF NOT EXISTS`）。

> 审计写入**失败不阻断主流程**（实现内部吞掉异常并降级为告警日志）。当前记录成功路径，登录失败亦留痕。

### 功能拓展（第二批，2026-09-24）

| 功能 | 说明 | 位置 |
| --- | --- | --- |
| **人工复核确认闭环** | 上批只做到「标记待复核」，本批补上医师**确认复核**：记录复核人、时间与意见，重复复核被拒；已确认的记录不再计入待复核统计 | 筛查记录 |
| **诊断报告（可打印/导出 PDF）** | 独立 A4 报告页：受检者信息、眼底原图与 Grad-CAM 热力图、分级结论与各分级概率表、医师复核、免责声明；配 `@media print` 样式，一键打印或另存为 PDF | 筛查记录 → 报告 |
| **批量删除** | 列表多选后批量删除（逐条执行并汇总成功/失败，二次确认提示将清理对象存储） | 筛查记录 |

**新增接口**：`PATCH /biz/screening-records/{id}/review`（权限 `biz:screening:review`，新增，DOCTOR 与 ADMIN 均具备）。

**新增字段**（`biz_screening_record`）：`review_status` / `reviewer` / `review_time` / `review_remark`；建表与**幂等增量迁移**（基于 `information_schema` 判断）均已并入 `sql/init/01_init_dr_screening.sql`。

> **本轮修复的一个真实缺陷**：`BizScreeningRecordConvert` 使用 `Map.of(...)` 构建的名称映射，在 `get(null)` 时**会抛 NPE**（`Map.of` 生成的不可变 Map 与 HashMap 行为不同），导致列表接口 500。已统一改为空安全查询 `nameOf(...)`，并同步修复导出与随访中的同类隐患（`LEVEL_NAMES` / `SUGGESTION_NAMES` 亦有此潜在问题）。

### 功能拓展（第三批，2026-09-24）

| 功能 | 说明 | 位置 |
| --- | --- | --- |
| **上传逐张进度与失败重试** | 上传改为**逐张独立提交**：队列展示每张影像的状态（等待 / 推理中 / 完成含耗时 / 失败含原因）与整体进度条；单张失败不影响其它影像，可一键「重试失败」 | 筛查上传 |
| **修改密码** | 个人中心新增改密表单（原密码 + 新密码 + 确认）；校验原密码一致、新密码 6-64 位且不得与原密码相同；修改动作记入操作日志 | 个人中心 |
| **统计环比** | 新增近 30 天 / 前 30 天筛查量与环比增长率，展示在筛查看板与统计分析的趋势卡片头部 | 筛查看板 / 统计分析 |

**新增接口**：`PUT /common/users/me/password`（仅需登录令牌）。统计 VO 增加 `recentTotal` / `prevTotal` / `growthRate`。

> **第三批同时修复两个既有缺陷**：
> 1. **新增用户一直不可用**：`AdminUserServiceImpl.createUser` 注释称主键与审计字段由 `AuditMetaHandler` 自动填充，但**代码库中并不存在该处理器**，导致插入时 `id` 为 null → `Column 'id' cannot be null` 报 500。已改为显式赋值 `id`/`deleteFlag`/时间戳。
> 2. **参数校验失败与不存在的路由都返回 500**：`GlobalExceptionHandler` 未处理 `MethodArgumentNotValidException` 与 `NoResourceFoundException`，导致表单填错（如密码过短）只得到笼统的「系统错误」。现已分别映射为 **400（带具体字段提示）** 与 **404**。

### 功能拓展（第四批，2026-09-24）

| 功能 | 说明 | 位置 |
| --- | --- | --- |
| **随访待办（导航大项）** | 汇总三类需医生处理的事项：**待人工复核**（置信度低于阈值）、**需转诊**（重度及以上）、**逾期未复诊**（>90 天未复查且中度以上）；顶部 KPI 给出各项数量，条目可直接跳转复核或打开诊断报告 | 筛查业务 → 随访待办 |
| **使用指南** | 面向使用者的说明页：快速上手 5 步、角色与数据权限、8 项核心功能说明、分级与转诊口径、5 条常见问题、免责声明；左侧目录锚点跳转 | 顶栏「使用指南」按钮 / 账户 → 使用指南 |
| **表格列宽自定义** | 全部列表启用 `border`，**拖动表头分隔线即可调整列宽**；列统一用 `min-width`，**初始宽度按内容与容器自适应**；拖动结果按页面记忆到 localStorage，列表右上角提供「恢复默认」 | 各列表页 |
| **修改密码改为模态框** | 个人中心由内嵌卡片改为**页头按钮 + 弹窗表单**，入口更清晰 | 个人中心 |

> 列宽记忆通过 `composables/useColumnWidths.ts` 实现：绑定 `@header-dragend` 按「表名 + 列标识」持久化，刷新后自动恢复。

### 功能拓展（第五批，2026-09-24）

| 项 | 说明 |
| --- | --- |
| **多标签页管理** | 内容区上方新增页签栏：点击菜单打开页签、切换保留页面状态（`KeepAlive`）；支持单个关闭、关闭其他、关闭全部；页签写入 `sessionStorage`，**刷新后不丢失** |
| **使用指南独立分组** | 「使用指南」从「账户」移出，放入新建的**「帮助」**导航分组 |
| **演示样例** | 筛查上传页新增「演示样例」按钮：一键载入内置样例眼底图（`public/samples/demo-fundus.png`）并自动完成筛查，便于演示与验收 |
| **演示数据脚本** | `tools/seed-demo-data.sh`：向业务库插入演示账号与 11 条筛查记录（覆盖五级分级、三种建议、低置信度待复核、多次随访、逾期未复诊、近/前 30 天环比）。**幂等**（按标记先清后插），`--clean` 仅清理，连接信息从环境变量读取 |

**对齐修复**：筛查上传左右两列底部对齐（列内末卡 `flex:1`）；个人中心两张卡片等高；字典管理左右两张卡片等高。

> **演示数据脚本注意点**：`biz_screening_record.image_key` 为 `NOT NULL`，故演示记录写入占位键 `demo/placeholder.png`（列表/统计/随访/待办不受影响，详情页会显示「影像不可用」）。若需要带真实影像的演示，请使用「演示样例」按钮或手工上传。

## 端到端联调结论（2026-09-14）

本期（阶段 5 完成后）在不实现阶段 6（H5）的前提下，对本地运行的完整链路做了一次端到端联调。基础设施 MinIO（9000/9001）、Redis（26379）、MySQL（3306）已就绪并在物理机后台监听；后端（8080）、模型服务（8000）、前端 dev（5173）依次启动后逐项验证。

### 验证环境

| 服务 | 端口 | 状态 |
| --- | --- | --- |
| MySQL | 3306 | 业务库 `dr_screening`，就绪 |
| Redis | 26379 | 令牌会话，就绪 |
| MinIO | 9000 / 9001 | 私有桶 `dr-screening`，就绪 |
| 后端 backend | 8080 | 启动成功（JDK 21，自动建桶 / 连库 / 播种账号） |
| 模型服务 | 8000 | FastAPI，CUDA cu130 + 未训练模型，`/health`=UP |
| 前端 dev | 5173 | Vite 6，`/api` 代理至后端 8080 |

### 验证清单（全部通过）

- 登录 `admin` / `doctor` → 200，返回 token + 角色 + 权限集 + 数据权限（ADMIN=ALL，DOCTOR=SELF）
- 筛查上传（doctor）→ 200，记录含 `resultLevel` / `confidence` / `probabilities` / 图片与热力图预签名 URL；模型推理 → MinIO 落库 → DB 全通
- 分页 / 详情 / 统计 → 正常
- Excel 导出（admin / doctor）→ 200，合法 `.xlsx`（含表头与数据行）
- 删除（doctor 删本人记录）→ 200，逻辑删除
- 数据权限 SELF 隔离 → doctor 仅见本人记录（0 条），admin（ALL）见全部；验证通过
- 功能权限 → doctor 访问 `/admin/users` → 403
- 前端代理 5173 → 8080 → 登录 / 数据正确

### 本轮修复

1. **Excel 导出 500（`NoSuchMethodError`）**：POI 5.3.0 编译依赖 `commons-compress 1.26.2`，而 Spring Boot 3.4.4 父 BOM 将其管理为 `1.24.0`，二者方法签名不兼容（`ZipArchiveOutputStream.putArchiveEntry`），导致导出时 `NoSuchMethodError` 并 500。已在后端聚合父 pom 的 `dependencyManagement` 中显式锁定 `commons-compress 1.26.2`，覆盖父 BOM；重新构建后 fat-jar 内含 1.26.2，导出恢复 200。
2. **医生缺导出权限（403）**：`PermissionResolver` 的 `DOCTOR_PERMISSIONS` 漏配 `BIZ_SCREENING_EXPORT`，与文档 / 前端约定（DOCTOR 拥有 `biz:screening:*`）不一致，导致医生导出被 403。已补入该权限（导出仍受 SELF 数据权限约束，仅本人记录）。

> 联调期间使用的是未经训练的随机初始化权重，分级结果为随机输出，仅用于打通管线；模型训练已于 2026-09-24 完成，见「模型训练」章节。

## 阶段进度

- [x] 阶段 0 脚手架与基础设施（后端编译通过、前端构建通过、部署与 SQL 脚本就绪）
- [x] 阶段 1 数据层与认证接口（MyBatis-Plus 配置、Entity/Mapper/Convert/DTO/VO、登录/字典/当前用户接口、AuthFilter + Redis Token、建表落库并端到端验证）
- [x] 阶段 2 认证与权限（@RequirePermission 拦截器对接业务接口、管理员用户/字典 CRUD、角色—权限映射与数据权限 SELF/ALL 落地、401/403 鉴权端到端验证）
- [x] 阶段 3 模型服务（FastAPI 推理管线 + 自实现 Grad-CAM + /predict、/cam、/health 接口；CUDA cu130 跑在 RTX 4050）
- [x] 阶段 4 业务核心（筛查上传→推理→落库→统计→导出：MinIO 私有桶存储、模型服务 HTTP 调用、数据权限 SELF/ALL、Excel 导出；后端 compile/package 验证通过）
- [x] 模型训练（2026-09-24：APTOS 2019 训练 MobileNetV3-Small，导出 `models/best_model.pth`，测试集 acc 0.8147 / macro F1 0.6555，推理服务零代码切换；另完成训练脚本性能优化，稳态提速 7.7 倍）
- [ ] 模型训练增强（可选：更大数据 / 更强增强 / 类别重采样，改善 LEVEL_1/3/4 的 F1）
- [x] 阶段 5 前端 PC 业务页面（看板 / 上传 / 记录 / 统计 / 个人中心 / 403：vue-tsc 类型检查 + vite build 通过，含权限对齐与导出二进制处理）
- [x] 前端界面重构（2026-09-24）：设计令牌体系 + 分组可折叠侧栏 + 三态切换 + 数据范围徽标 + 通用组件抽取（AppIcon/PageHeader/StatCard），7 个页面统一卡片化布局，四档响应式（详见「前端界面与设计系统」）
- [x] 功能拓展（2026-09-24）：用户管理 / 字典管理 / 操作日志（系统管理三页，补齐此前「有权限无界面」的缺口）、低置信度人工复核、患者随访（详见「功能拓展」）
- [x] 端到端联调与修复（2026-09-14）：导出 `NoSuchMethodError` 修复 + 医生导出权限修复，全链路验证通过（见上文「端到端联调结论」）
- [ ] 阶段 6 H5（后续可选）
- [ ] 阶段 7 部署（本地运行 + Docker 预留）
