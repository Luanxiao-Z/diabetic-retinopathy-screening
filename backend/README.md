# drs-backend — 糖尿病视网膜病变智能筛查系统（后端）

后端聚合工程。基于 **Java 21 + Spring Boot 3.4.4** 的 Maven 多模块项目，
对外提供 REST 接口（统一前缀 `/api/v1`），承担认证鉴权、筛查业务编排、
数据持久化、审计留痕与模型服务调用。

> 项目总体说明、功能清单、部署与验收相关文档见**仓库根目录 `README.md`**；
> 本文档只覆盖后端目录自身的构建、配置与开发约定。

---

## 一、技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 / 运行时 | Java 21 |
| 框架 | Spring Boot 3.4.4（Web、Validation、AOP） |
| 持久层 | MyBatis-Plus + MySQL 8 |
| 缓存 / 会话 | Redis（UUID Token + Session，**非 JWT**） |
| 对象存储 | MinIO（眼底图与热力图） |
| 接口文档 | Knife4j / SpringDoc OpenAPI |
| 验证码 | easy-captcha |
| Excel 导出 | Apache POI |
| 构建 | Maven（多模块聚合） |

---

## 二、模块结构

```
backend/
├── pom.xml                       聚合 POM（版本与插件统一在此管理）
├── drs-dependencies/
│   └── drs-dependencies-bom      依赖版本 BOM，各模块不再各自指定版本
├── drs-module/
│   ├── drs-module-common         公共层：统一响应体、异常、审计契约、常量、工具
│   ├── drs-module-security       安全层：认证过滤器、权限注解、上下文、数据范围
│   ├── drs-module-system         系统域：用户、角色、字典、操作日志、认证与注册
│   └── drs-module-screening      业务域：筛查记录、人工复核、随访、统计、模型客户端
└── drs-app/
    └── drs-app-server            启动模块：主类 DrsApplication、application.yml
```

**依赖方向**（单向，不允许反向依赖）：

```
drs-app-server ──> drs-module-system ──> drs-module-security ──> drs-module-common
                └─> drs-module-screening ─┘
```

各模块内部统一遵循 `controller → service → mapper → entity` 分层，
跨层传输使用 `dto`（入参）/ `vo`（出参），转换逻辑集中在 `convert`。

---

## 三、环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | 编译与运行均需 21 |
| Maven | 3.9+ | |
| MySQL | 8.0 | 库名 `dr_screening`，建表脚本见 `../sql/` |
| Redis | 7.x | **本项目使用容器 `fjzhmz-redis` 映射的 26379 端口，非默认 6379** |
| MinIO | 最新 | 容器 `fjzhmz-minio`，端口 9000（控制台 9001） |
| 模型服务 | — | 需先启动，默认 `http://127.0.0.1:8000` |

---

## 四、快速开始

### 1. 准备依赖服务

MySQL、Redis、MinIO 三个外部依赖需先就绪（Redis / MinIO 由 Docker 提供）。

### 2. 配置环境变量

`application.yml` 中数据库密码、Redis 密码、MinIO 凭据**均通过环境变量注入**，
不写明文。启动前需设置：

```bash
export DB_PASSWORD=<MySQL root 密码>
export REDIS_PASSWORD=<Redis 密码>
export MINIO_ACCESS_KEY=<MinIO AccessKey>
export MINIO_SECRET_KEY=<MinIO SecretKey>
```

### 3. 构建

```bash
# 在 backend/ 目录下
mvn clean install -DskipTests
```

产物：`drs-app/drs-app-server/target/drs-app-server-1.0.0.jar`

> **Windows / PowerShell 注意**：PowerShell 会剥离命令行中的 `-Dx=y` 参数，
> 需将其放入环境变量：
> ```powershell
> $env:MAVEN_OPTS="-DskipTests"; mvn clean install
> ```

### 4. 启动

```bash
java -jar drs-app/drs-app-server/target/drs-app-server-1.0.0.jar --server.port=8080
```

服务默认监听 **8080**。验证：`curl http://127.0.0.1:8080/api/v1/auth/captcha` 应返回 200。

---

## 五、接口约定

### 统一响应体

```json
{ "code": 200, "msg": "success", "data": { } }
```

- **业务异常维持 HTTP 200**，错误信息放在信封的 `code` / `msg` 中；
  仅 `401` / `403` 同时返回对应的 HTTP 状态码
- Jackson 配置为 `non_null`，**null 字段不下发**，前端类型需按可选字段处理

### 认证与权限

| 项 | 约定 |
| --- | --- |
| 令牌传递 | 请求头 `X-Access-Token` |
| 会话存储 | Redis，UUID Token |
| 匿名路径 | 前缀 `/api/v1/auth/`（新增匿名接口置于此前缀下即自动放行） |
| 权限控制 | 方法级注解 `@RequirePermission`，编码常量集中在 `PermissionConstants` |
| 数据范围 | `ADMIN` → `ALL`（全量）；`DOCTOR` → `SELF`（仅本人） |

### 分页

分页参数名为 **`current` / `pageSize`**（不是 `pageNum`），
响应体为 `PageResult<T>`：`{ total, current, pageSize, list }`。

---

## 六、开发约定（易踩坑，务必阅读）

以下均为本项目实际踩过的坑，新增代码时请对照检查：

1. **无 `MetaObjectHandler` 自动填充**
   新建实体必须**显式设置** `id` / `deleteFlag` / 时间戳，否则报
   `Column 'id' cannot be null`。

2. **`updateById` 无法把字段更新为 `null`**
   MyBatis-Plus 默认字段策略为 `NOT_NULL`，会把 null 排除在 SET 子句之外。
   需要「清空某字段」时必须改用 `LambdaUpdateWrapper` 显式 `.set(...)`。

3. **`Map.of(...)` 的 `get(null)` 会抛 NPE**
   名称映射查询一律使用空安全的 `nameOf(...)`（见 `BizScreeningRecordConvert`）。

4. **实体与 DTO 多为手写 getter/setter**
   新增字段时须**同时补访问器**（VO 使用 Lombok，实体与 DTO 未用）。

5. **密码统一走 `PasswordUtil`**
   SHA-256 + 随机盐，存储格式 `salt:hash`，不要自行实现哈希。

6. **审计统一走 `OperationLogRecorder`**
   契约定义在 `common` 模块、实现在 `system` 模块；
   **实现内部必须吞掉自身异常**，避免审计失败影响主流程。

7. **改后端前必须先停 8080**
   否则 `mvn clean` 无法删除被占用的 `target` 目录。

8. **构建必须用原生 `mvn.cmd`**
   在 PowerShell 中调用，并将 `-DskipTests` 放入 `$env:MAVEN_OPTS`（原因见上文）。

---

## 七、关键实现说明

### 人工复核判定

判定依据是**归一化预测熵**（`uncertainty`），**不是 top1 置信度**：

```
不确定性 = −Σ pᵢ·ln pᵢ / ln 5      （取值范围 0 ~ 1）
```

- 阈值常量：`BizScreeningRecordConvert.REVIEW_UNCERTAINTY_THRESHOLD = 0.15`
- 语义：**熵 ≥ 阈值**即需人工复核
- **阈值与模型绑定**：任何模型重训后必须重跑 `tools/fit_review_threshold.py` 重新标定
- `confidence` 仅用于界面展示，不参与任何判定逻辑

### 登录验证码与注册限流

| 项 | 实现 |
| --- | --- |
| 验证码 | `GET /auth/captcha`，easy-captcha 生成，文本存 Redis（TTL 300s），**一次性消费** |
| 注册限流 | `RateLimiter` 双层限流：全站每分钟 30 次 + 同一 IP 每 10 分钟 5 次，超限 `code=429` |
| IP 提取 | 优先 `X-Forwarded-For` 首段，便于部署在 Nginx 之后 |

### 随访待办聚合

`GET /biz/screening-records/todos` 聚合三类待办（待人工复核 / 需转诊 / 逾期未复诊）。
三类来源不同（两个来自筛查记录、一个来自患者聚合），**无法用单条 SQL 分页**，
故统一为 `TodoItemVO` 后在内存合并分页；单类查询均加 `LIMIT 1000` 上限。

---

## 八、常见问题

| 现象 | 原因与处理 |
| --- | --- |
| `Column 'id' cannot be null` | 新建实体未显式设置主键，见「开发约定」第 1 条 |
| 字段清空后仍是旧值 | `updateById` 忽略 null，改用 `LambdaUpdateWrapper`，见第 2 条 |
| `mvn clean` 报无法删除 target | 8080 端口仍被占用，先停服务 |
| 启动报 Redis 连接失败 | Redis 端口是 **26379** 而非 6379；确认 Docker 容器 `fjzhmz-redis` 已运行 |
| 启动报 MinIO 连接失败 | 确认容器 `fjzhmz-minio` 已运行、`MINIO_*` 环境变量已设置 |
| 上传筛查报模型服务不可用 | 模型服务（默认 8000）未启动 |
| 登录返回「验证码已过期」 | 验证码一次性消费，失败后需点击图片刷新重新获取 |

---

## 九、相关文档

| 文档 | 位置 |
| --- | --- |
| 项目总览、功能与部署 | `../README.md` |
| 系统实现说明（含设计取舍） | `../docs/系统实现说明.md` |
| 数据库建表脚本 | `../sql/` |
| 接口规范与验收要求 | `../spec/` |
