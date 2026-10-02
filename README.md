# 糖尿病视网膜病变（DR）智能筛查系统

面向基层医疗与眼科筛查场景的 **AI 辅助眼底筛查系统**。上传眼底照片后，系统自动完成糖尿病视网膜病变五级分级（LEVEL_0 正常 ~ LEVEL_4 增殖期），生成 Grad-CAM 可解释热力图，并给出分级转诊建议；同时提供筛查记录管理、低置信度人工复核、患者纵向随访、统计分析与诊断报告导出等业务能力。

> **免责声明**：本项目为医学人工智能课程项目，AI 筛查结果**仅供临床参考，不能替代执业医师的诊断意见**。

---

## 一、功能特性

### 筛查业务
- **筛查上传**：选择眼底图后先入队预览（缩略图可点击放大确认），再点击「开始筛查」逐张独立推理；单张失败不影响其它影像并可单独重试，队列实时展示每张状态与耗时
- **筛查记录**：按患者、分级、复核状态与时间检索；复核状态区分为「待复核 / 已复核 / 无需复核」；支持批量删除与 Excel 导出；表头可拖拽调整列宽并按页记忆；**双击行可打开详情**
- **随访待办**：以分页表格呈现三类待办——待人工复核、需转诊（重度及以上）、逾期未复诊（>90 天且中度以上）；支持类型切换、患者姓名筛选与分页；「全部」视图额外标注每项归属类型
- **患者随访**：按患者归并历次筛查，列表分列展示「分级变化」（前次 → 最近）与「变化趋势」（进展 / 好转 / 持平 / 首次），并提供随访时间线
- **人工复核**：以模型**不确定性**（归一化预测熵，阈值 **0.18**）识别需复核结果，医师核对后确认复核并留存意见，形成可追溯闭环
- **诊断报告**：A4 版式报告，含受检者信息、眼底原图与热力图、分级结论与各分级概率表、医师复核与免责声明，可打印或另存为 PDF

### 综合看板
- **工作台**：简要概览与业务入口——累计筛查、转诊率、需转诊、待人工复核四项关键指标（含环比），待办事项摘要（待复核 / 需转诊 / 逾期未复诊三类数量与直达入口），以及常用业务快捷入口
- **统计分析**：详细统计数据——统计明细表（累计 / 近 30 天 / 前 30 天 / 环比 / 转诊率 / 各建议类型 / 待复核）、分级分布、转诊建议分布与近 30 天趋势
- **模型信息**：模型版本、骨干、推理设备与训练指标（准确率、宏平均 F1、逐类 F1、逐轮训练曲线）

### 系统管理
- **用户管理**：账号分页查询、新增、编辑、启停与逻辑删除
- **字典管理**：字典域与其下字典项的维护（增删改、排序、级联删除）
- **操作日志**：登录、筛查上传/删除/导出、人工复核、账号变更等关键动作留痕，含操作人、结果、耗时与 IP

### 通用能力
- **账号注册**：登录页提供自助注册入口，注册即获得医生角色（数据范围仅限本人）；用户名唯一、密码需含字母与数字；注册接口受**双层限流**（同一 IP 每 10 分钟 5 次、全站每分钟 30 次）
- **登录安全**：登录需输入**图形验证码**（5 分钟有效、一次性消费，校验后立即失效）
- **权限体系**：轻量化角色映射（ADMIN / DOCTOR）+ 数据权限（ALL / SELF），不建 RBAC 表
- **多标签页**：页签切换保留页面状态，支持关闭单个 / 其他 / 全部，刷新后不丢失
- **列表交互**：双击行执行该列表的默认操作（筛查记录 → 详情、用户 → 编辑、字典项 → 编辑、患者随访 → 时间线）
- **数据实时刷新**：完成筛查、删除或复核后，各列表与看板在切回时自动更新
- **使用指南与关于系统**：面向使用者的操作说明、FAQ、技术架构与第三方组件说明
- **个人中心**：账号信息（用户名、真实姓名、手机号、角色、数据范围、账号创建时间）与权限条目（中文）；支持**编辑本人资料**（真实姓名、手机号）与修改密码

> **时间展示约定**：界面时间统一为 `yyyy-MM-dd HH:mm:ss`（空格分隔、无 `T`）；
> 仅操作日志等面向排障的场景保留 ISO8601 写法。

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

### 0. 前置条件

| 组件 | 版本要求 | 检查命令 |
| --- | --- | --- |
| JDK | **21** | `java -version` |
| Maven | 3.9.x | `mvn -v` |
| Node.js | 18+（推荐 22） | `node -v` |
| Yarn | 1.22 | `yarn -v` |
| Python | 3.12+ | `python --version` |
| MySQL / Redis / MinIO | 8.0 / 7 / 最新 | 见下 |

**基础设施必须先就绪**——本项目不自带数据库、缓存与对象存储。启动前确认以下端口均在监听：

```bash
# Windows
netstat -ano | findstr "3306 26379 9000 9001"
# Linux / macOS
netstat -ano | grep -E ":(3306|26379|9000|9001)"
```

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| MySQL | 3306 | 业务库 `dr_screening` |
| Redis | 26379 | 会话缓存（注意不是默认的 6379） |
| MinIO API / Console | 9000 / 9001 | 对象存储 |

> 端口缺失时请先启动基础设施；若希望一键起停全部服务，可使用 `deploy/` 下的容器编排
> （见 [deploy/README.md](deploy/README.md)）。

### 1. 配置连接信息

连接信息一律通过**环境变量**注入，**请勿将明文写入版本库**：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | MySQL 密码（业务库 `dr_screening`，以 `root` 连接） |
| `REDIS_PASSWORD` | Redis 密码 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 凭据（后端启动时自动创建私有桶 `dr-screening`） |

Windows（PowerShell，`setx` 写入用户级变量，**需新开终端生效**）：

```powershell
setx DB_PASSWORD "你的数据库密码"
setx REDIS_PASSWORD "你的 Redis 密码"
setx MINIO_ACCESS_KEY "你的访问键"
setx MINIO_SECRET_KEY "你的密钥"
```

Linux / macOS：

```bash
export DB_PASSWORD=... REDIS_PASSWORD=... MINIO_ACCESS_KEY=... MINIO_SECRET_KEY=...
```

### 2. 初始化数据库

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < sql/init/01_init_dr_screening.sql
```

> 脚本**幂等**，可重复执行：包含建表、字典初始化与增量迁移判断，已建库的环境直接执行即可完成升级。
> Windows 下若出现中文乱码，请追加 `--default-character-set=utf8mb4`。

### 3. 启动模型服务（端口 8000）

```bash
cd model-service
python -m venv .venv && source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

验证：

```bash
curl http://127.0.0.1:8000/health
# 期望：{"status":"UP","model":"aptos2019-mobilenetv3s-1.0.0","device":"cuda","trained":true,...}
```

> `requirements.txt` 默认拉取 CUDA 版 PyTorch；无 NVIDIA GPU 时请将末两行改为 `+cpu` 轮子，
> 模型服务支持 CPU 回退（`device` 会显示 `cpu`）。
> 若权重文件 `model-service/models/best_model.pth` 缺失，服务仍可启动但 `trained=false`，
> 推理结果为随机初始化输出，**仅可用于联调**。

### 4. 启动后端（端口 8080）

```bash
bash tools/mvn.sh clean install -DskipTests          # 构建 fat-jar
cd backend/drs-app/drs-app-server/target
java -jar drs-app-server-1.0.0.jar --server.port=8080
```

首次启动会自动完成：连接数据库、创建 MinIO 私有桶、幂等播种种子账号。

验证：

```bash
curl http://127.0.0.1:8080/api/v1/common/health
# 期望：{"code":200,"msg":"success","data":"UP"}
```

> **改后端代码前必须先停止 8080 端口**，否则 `mvn clean` 无法删除被运行进程占用的 `target` 目录。
> 若 PATH 上的 `java` 不是 21，请显式指定 JDK 21 的完整路径启动。

### 5. 启动前端（端口 5173）

```bash
cd frontend/web
yarn install
yarn dev        # 开发服务器 http://localhost:5173，/api 代理至后端 8080
yarn build      # 生产构建，产物位于 dist/
```

### 6. 访问与登录

浏览器打开 <http://localhost:5173>。

| 账号 | 密码 | 角色 | 数据权限 |
| --- | --- | --- | --- |
| `admin` | `admin123` | 管理员 | 全部数据（ALL） |
| `doctor` | `doctor123` | 医生 | 仅本人数据（SELF） |

> 账号由后端启动时幂等播种，已存在则跳过。也可在登录页点击「立即注册」自助创建医生账号：
> 注册账号的角色固定为 DOCTOR、数据范围固定为 SELF，**管理员账号只能由既有管理员创建**。

### 7. 启动顺序与依赖关系

```
MySQL / Redis / MinIO（前置，必须先就绪）
          ↓
   模型服务 :8000  ──HTTP──►  后端 :8080  ──HTTP──►  前端 :5173
```

- 三个应用服务**无强制启动先后**：后端不依赖模型服务启动，仅在实际推理时调用它
  （不可达时降级返回 `code=503`，前端可对失败影像单独重试）。
- 建议按「模型服务 → 后端 → 前端」顺序启动，便于逐段用上面的验证命令确认。
- 建议为三个服务各开一个终端窗口，便于观察日志与单独重启。

### 8. 常见问题

| 现象 | 原因与处理 |
| --- | --- |
| 上传后提示「无法连接模型推理服务」 | 模型服务（8000）未启动或不可达；启动后在筛查上传页点击「重试失败」 |
| 后端启动报数据库连接失败 | 确认 MySQL 在线，且 `DB_PASSWORD` 已在**新终端**中生效 |
| 登录提示「用户名或密码错误」 | 种子账号在后端**首次启动**时播种，确认数据库已初始化 |
| 页面分页、表格空态出现英文 | 需全局注入 Element Plus 中文语言包（`main.ts` 中的 `zhCn`） |
| `mvn clean` 报无法删除 `target` | 8080 仍有进程占用，先停止后端再构建 |
| 导入 `torch` 偶发 `WinError 32` | Windows 下 DLL 被安全软件扫描占用，重试即可（非依赖损坏） |
| 打印诊断报告时带出侧栏/顶栏 | 打印样式已全局隐藏壳层元素；若仍出现，请检查浏览器是否禁用了页面样式 |
| 验证码看不清 | 点击验证码图片可刷新；验证码 5 分钟有效且**校验后立即失效**，登录失败需重新输入 |
| 注册提示「注册过于频繁」 | 已启用限流（同一 IP 每 10 分钟最多 5 次、全站每分钟最多 30 次），等待后重试即可 |

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
| POST | `/auth/sessions` | 登录，返回令牌、角色、权限集与数据权限（需携带图形验证码） |
| DELETE | `/auth/sessions` | 登出（令牌失效） |
| GET | `/auth/captcha` | 获取图形验证码（匿名，5 分钟有效、一次性消费） |
| POST | `/auth/users` | 自助注册普通医生账号（匿名接口，受 IP 与全局双层限流） |
| GET | `/common/users/me` | 当前用户档案（含真实姓名、手机号、账号创建时间） |
| PUT | `/common/users/me` | 修改本人资料（仅真实姓名与手机号） |
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
| GET | `/biz/screening-records/todos` | 随访待办分页（按类型聚合三类待办） |
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
| `biz_screening_record` | 筛查记录（分级、置信度、不确定性、各分级概率、转诊建议、复核信息、对象键） |
| `sys_operation_log` | 操作审计日志 |

建表与字典初始化脚本：`sql/init/01_init_dr_screening.sql`（幂等，含增量迁移判断）。

---

## 十一、数据集

系统使用 **APTOS 2019 糖尿病视网膜病变数据集**（2019 APTOS Blindness Detection）训练与评估模型。

### 数据集概况

| 项 | 说明 |
| --- | --- |
| 名称 | APTOS 2019 Blindness Detection |
| 来源 | Kaggle 竞赛，眼底图像由印度 Aravind Eye Hospital 提供 |
| 任务 | 眼底图像 DR 五级分级（0 = 无病变 … 4 = 增殖期） |
| 本项目使用版本 | `sovitrath` 发布的 224×224 预处理版（已裁去黑边并统一尺寸） |
| 图像总数 | 3662 张 |
| 标注文件 | `train.csv`（字段 `id_code, diagnosis`） |
| 图像格式 | PNG，224×224，RGB |

### 类别分布

| 分级 | 标签值 | 目录名 | 图像数 | 占比 |
| --- | --- | --- | --- | --- |
| LEVEL_0 正常 | 0 | `No_DR` | 1805 | 49.3% |
| LEVEL_1 轻度 | 1 | `Mild` | 370 | 10.1% |
| LEVEL_2 中度 | 2 | `Moderate` | 999 | 27.3% |
| LEVEL_3 重度 | 3 | `Severe` | 193 | 5.3% |
| LEVEL_4 增殖期 | 4 | `Proliferate_DR` | 295 | 8.1% |
| **合计** | | | **3662** | 100% |

> 类别分布明显不均衡（LEVEL_0 占近一半，LEVEL_3 仅 5.3%）。训练脚本因此采用**分层抽样**划分
> 训练/验证/测试集，并对损失函数施加**类别权重**；这也是 LEVEL_3 的 F1 偏低（0.378）的主要原因。

### 目录约定

```
datasets/aptos2019_224x224/
├── train.csv                  标注文件（id_code, diagnosis）
└── colored_images/            按分级分目录存放
    ├── No_DR/                 1805 张
    ├── Mild/                  370 张
    ├── Moderate/              999 张
    ├── Severe/                193 张
    └── Proliferate_DR/        295 张
```

> `datasets/` **不纳入版本控制**（已在 `.gitignore` 中忽略），需自行下载后放置。

### 获取方式

**方式一：Kaggle 数据集页（推荐）**

```bash
# 需先配置 Kaggle API 凭据（~/.kaggle/kaggle.json）
kaggle datasets download -d sovitrath/diabetic-retinopathy-224x224-2019-data
unzip diabetic-retinopathy-224x224-2019-data.zip -d datasets/
```

数据集页：<https://www.kaggle.com/datasets/sovitrath/diabetic-retinopathy-224x224-2019-data>

**方式二：原始竞赛数据**

竞赛页：<https://www.kaggle.com/c/aptos2019-blindness-detection>

下载的是原始分辨率眼底图（含黑边），需自行裁圆并缩放至 224×224，或改用其他预处理版本。

**放置位置**：确保最终目录结构为 `datasets/aptos2019_224x224/{train.csv, colored_images/*}`。
训练脚本通过 `--data-root ../datasets/aptos2019_224x224` 读取（相对 `model-service/` 目录）。

> **网络提示**：Kaggle 在中国大陆通常需经代理访问；`kaggle` CLI 支持 `--proxy` 参数，
> 或通过环境变量 `HTTPS_PROXY` / `HTTP_PROXY` 指定代理。

### 使用许可与引用

数据集版权归原始提供方所有，**仅限科研与教学用途，不得用于商业用途**，使用时请遵循
Kaggle 竞赛页面载明的规则。学术引用建议注明来源：

> APTOS 2019 Blindness Detection, Kaggle. Data provided by Aravind Eye Hospital, India.

---

## 十二、模型说明

| 项 | 说明 |
| --- | --- |
| 骨干网络 | MobileNetV3-Small（5 类分类头） |
| 训练数据 | APTOS 2019（sovitrath 224×224 预处理版，3662 张，5 类，详见「十一、数据集」） |
| 训练入口 | `model-service/training/train.py`（分层抽样、类别加权、按验证集宏 F1 选优） |
| 权重路径 | `model-service/models/best_model.pth`（**已纳入版本控制**，约 5.9 MB；缺失时回退随机初始化） |
| 分级映射 | `LEVEL_0/1 → REVIEW`、`LEVEL_2 → CLINIC`、`LEVEL_3/4 → REFERRAL` |

```bash
cd model-service
python -m training.train --data-root ../datasets/aptos2019_224x224 \
  --epochs 30 --batch-size 32 --lr 3e-4
```

训练完成后导出 `models/best_model.pth` 与 `models/train_metrics.json`（两者均已纳入版本控制），
推理服务**零代码切换**。模型性能指标可在系统内「综合看板 → 模型信息」页查看。

> 当前已训练权重在测试集（367 张）上的表现为：准确率 **0.8147**、宏平均 F1 **0.6555**；
> 最佳验证轮次出现在第 29 轮，继续增加训练轮次会过拟合，提升精度应转向数据规模与增强策略。

### 不确定性与人工复核阈值

系统除分级与置信度外，还输出**归一化预测熵**作为不确定性指标：

> 不确定性 = −Σ pᵢ·ln pᵢ / ln 5 　（pᵢ 为第 i 类的 softmax 概率，取值范围 0 ~ 1）

- `0` 表示模型完全确定（某一类概率为 1）；`1` 表示五类概率均匀、完全不确定
- 该值随筛查记录一并落库（`biz_screening_record.uncertainty`），在结果卡、筛查记录列表与诊断报告中展示

**为何不以置信度作为复核依据**：深度网络的 softmax 普遍饱和。在**完整测试集（367 张）**上，
按「置信度 < 0.70」判定仅触发 12.0%，却只覆盖 **23.5%**（16/68）的错分样本，复核机制形同虚设。

归一化熵利用了完整的概率分布形状，对「高置信度的错误预测」更敏感：

| 判定规则 | 触发率 | 错分召回 |
| --- | --- | --- |
| 置信度 < 0.70（旧） | 12.0% | 23.5%（16/68） |
| 归一化熵 ≥ 0.20 | 27.0% | 60.3%（41/68） |
| **归一化熵 ≥ 0.18（现行）** | **28.6%** | **61.8%（42/68）** |
| 归一化熵 ≥ 0.25 | 24.3% | 57.4%（39/68） |

阈值 **0.18** 在「触发率 ≤ 30%」的约束下取得最高召回（与 0.15 持平但触发更少），
定义于 `BizScreeningRecordConvert.REVIEW_UNCERTAINTY_THRESHOLD`。
标定脚本：`tools/fit_review_threshold.py`（复用与训练一致的划分：分层抽样 8:1:1、seed=42）。

> **已知局限**：仍有 38.2% 的错分样本熵极低（18/68 的熵 < 0.05），属「高置信度错误」，
> 任何基于输出分布的不确定性指标都无法识别，需从模型层面改进（更多数据、更强特征表达）。

---

## 十三、开发规范与质量

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

## 十四、部署

- **本地运行**：见「六、快速开始」，三个服务分别启动，基础设施由本机提供
- **容器化**：`deploy/` 提供 `docker-compose.yml`、三个 Dockerfile 与 Nginx 配置，
  一键启动全部六个服务（MySQL / Redis / MinIO / 模型服务 / 后端 / 前端），
  详见 [deploy/README.md](deploy/README.md)

---

## 十五、辅助脚本

| 脚本 | 用途 |
| --- | --- |
| `tools/mvn.sh` | Git Bash 下 Maven 包装脚本（解决 POSIX 路径问题） |
| `tools/seed-demo-data.sh` | 生成演示数据（账号 + 筛查记录），幂等，支持 `--clean` |

---

## 十六、许可证

本项目采用 [MIT License](LICENSE)。第三方组件遵循各自开源许可，清单见系统内「帮助 → 关于系统」页。
