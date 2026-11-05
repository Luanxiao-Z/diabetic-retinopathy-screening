# 后端 API 权限控制规范

> **配套规范**：前端页面级与元素级权限控制见 [前端页面权限控制规范](前端页面权限控制规范.md)；接口响应结构见 [后端应用开发规范](../backend/后端应用开发规范.md)。

## 一、背景与目标

本项目不引入 Spring Security，采用**自定义认证过滤器 + 权限拦截器**实现访问控制，设计目标是：

- 认证与鉴权逻辑集中在 `drs-module-security` 单模块内，其余业务模块只依赖注解与上下文；
- 采用**轻量化角色映射**，不建 RBAC 表、不维护权限树；
- 认证采用 **UUID Token + Redis Session**（非 JWT），令牌通过请求头 `X-Access-Token` 传递；
- 权限编码前后端一致，后端为权威来源。

实际实现只覆盖两类场景：

1. **匿名可访问** —— 登录、验证码、注册等 `/api/v1/auth/` 下的接口，以及健康检查 `/api/v1/common/health`；
2. **登录后访问** —— 其余全部接口，必须携带有效令牌；带 `@RequirePermission` 的接口额外校验功能权限，涉及业务数据的接口再叠加数据权限。

## 二、核心架构

```
请求
  └─► AuthFilter（OncePerRequestFilter）
        ├─ 匿名路径（/api/v1/auth/**、/api/v1/common/health）→ 直接放行
        ├─ 读取 X-Access-Token → TokenService 查询 Redis Session
        ├─ 令牌无效/过期 → 401
        └─ 有效 → 写入 AuthContext（AuthPrincipal）→ 放行
              └─► PermissionInterceptor（HandlerInterceptor）
                    ├─ 读取 @RequirePermission 声明的编码
                    ├─ 权限不足 → 403
                    └─ 通过 → Controller
                          └─► Service（按 DataScope 拼接查询条件）
```

核心类（均位于 `cn.edu.fzu.drs.module.security`）：

| 类 | 职责 |
| --- | --- |
| `filter/AuthFilter` | 令牌校验、匿名放行、写入认证上下文 |
| `interceptor/PermissionInterceptor` | 解析 `@RequirePermission` 并校验功能权限 |
| `annotation/RequirePermission` | 声明接口所需权限编码 |
| `context/AuthContext` | 基于 `ThreadLocal` 保存当前登录主体 |
| `model/AuthPrincipal` | 登录主体（用户 ID、用户名、角色、数据范围、权限集） |
| `core/PermissionResolver` | 角色 → 权限集映射（ADMIN / DOCTOR） |
| `core/DataScopeType` | 数据范围枚举（ALL / SELF） |
| `util/DataScopeUtils` | 数据权限条件拼接辅助 |
| `service/TokenService`、`service/impl/RedisTokenServiceImpl` | 令牌签发、校验、续期与注销 |
| `config/DrsSecurityAutoConfiguration` | 自动装配过滤器与拦截器 |
| `config/RedisConfig` | Redis 连接配置 |

> 匿名路径由 `AuthConstants.ANONYMOUS_PREFIX = "/api/v1/auth/"` 判定。新增匿名接口置于该前缀下即自动放行，无需修改过滤器。

## 三、URL 路径前缀规则

路径格式为 `/api/v1/{场景前缀}/{资源路径}`。资源路径只含资源名词（小写、连字符），操作由 HTTP 方法表达。

| 前缀 | 认证策略 | 说明 |
| --- | --- | --- |
| `/api/v1/auth/**` | 匿名 | 会话（`/sessions`）、验证码（`/captcha`）、注册（`/users`） |
| `/api/v1/common/**` | 登录 | 当前用户档案与权限、字典查询；其中 `/common/health` 例外放行 |
| `/api/v1/admin/**` | 登录 + 功能权限 | 用户、字典、操作日志等管理类资源 |
| `/api/v1/biz/**` | 登录 + 功能权限 + 数据权限 | 筛查记录、统计、待办、患者随访等业务资源 |

## 四、认证约定

- 令牌为 **UUID 字符串**，服务端在 Redis 中保存 Session，设置过期时间；登出时删除 Session。
- 请求头固定为 `X-Access-Token`；不使用 `Authorization`、不使用 JWT。
- 令牌缺失或无效返回 `401`（HTTP 401 + 信封 `code=401`）；权限不足返回 `403`（HTTP 403 + 信封 `code=403`）。
- 认证上下文仅在当前请求线程内有效，异步任务不得直接读取 `AuthContext`。

## 五、功能权限

### 5.1 权限编码

权限编码定义在 `drs-module-common` 的 `PermissionConstants`，全部小写、以冒号分段：

| 编码 | 适用角色 | 说明 |
| --- | --- | --- |
| `common:dict:view` | ADMIN / DOCTOR | 字典查询 |
| `admin:user:view`、`admin:user:edit` | ADMIN | 用户查询与维护 |
| `admin:dict:view`、`admin:dict:edit` | ADMIN | 字典域与字典项维护 |
| `admin:log:view` | ADMIN | 操作日志查询 |
| `biz:screening:create` | ADMIN / DOCTOR | 上传筛查 |
| `biz:screening:view` | ADMIN / DOCTOR | 记录查询、详情、统计、待办、患者随访 |
| `biz:screening:export` | ADMIN / DOCTOR | 导出 Excel |
| `biz:screening:delete` | ADMIN / DOCTOR | 删除记录 |
| `biz:screening:review` | ADMIN / DOCTOR | 人工复核确认 |

> **编码精简原则**：统计、待办、患者随访等查询类能力复用 `biz:screening:view`，不单独定义编码。

### 5.2 角色映射

角色只有 `ADMIN` 与 `DOCTOR` 两种，由 `PermissionResolver` 静态映射出权限集，不建角色表、权限表或关联表：

- `ADMIN`：拥有上表全部权限，数据范围 `ALL`；
- `DOCTOR`：拥有 `common:dict:view` 与全部 `biz:screening:*` 权限，数据范围 `SELF`。

新增权限编码时，必须同步更新 `PermissionConstants`、`PermissionResolver` 与前端权限声明。

### 5.3 `@RequirePermission` 用法

```java
@RequirePermission(PermissionConstants.ADMIN_USER_EDIT)
@PutMapping("/admin/users/{id}")
public Result<Void> update(@PathVariable String id, @RequestBody @Valid UserUpdateDTO dto) {
    // ...
}
```

- 注解标注在 Controller 方法上；未标注的接口只要求登录，不校验功能权限。
- `PermissionInterceptor` 从 `AuthPrincipal` 取权限集比对；前端 `v-permission` 仅用于体验，后端必须独立校验。

## 六、数据权限

- 数据范围分 `ALL`（全量）与 `SELF`（仅本人创建的记录）两档，由 `DataScopeType` 定义。
- 在 Service 层依据登录主体拼接查询条件（如 `user_id = 当前用户`），不得依赖前端传参或仅在 SQL 层面遗漏过滤。
- `ADMIN` 为 `ALL`；`DOCTOR` 为 `SELF`。注册账号固定为 `DOCTOR` + `SELF`，不接受前端传入角色，避免越权。

## 七、异常处理与响应

- 响应体统一为 `{ code, msg, data }`。
- **业务异常维持 HTTP 200 + 信封 `code`**（如参数错误 `400`、系统错误 `500`、限流 `429`、模型服务不可用 `503`）；
  仅 `401` / `403` 同时返回对应 HTTP 状态码。
- `GlobalExceptionHandler` 统一捕获 `UnauthorizedException`、`AccessDeniedException`、`BusinessException` 与参数校验异常，转换为统一响应体。
- 异常信息不得包含 SQL、堆栈或内部地址。

## 八、自动装配

`DrsSecurityAutoConfiguration` 负责注册 `AuthFilter` 与 `PermissionInterceptor`、配置拦截路径与放行路径；`RedisConfig` 提供 `Jedis` 连接。业务模块无需重复声明这些 Bean。

## 九、限流与验证码

- 登录必须携带图形验证码（easy-captcha），验证码文本存于 Redis（`drs:auth:captcha:{key}`），TTL 300 秒，**一次性消费**：校验后无论成功失败立即删除。
- 注册接口受双层限流：全站每分钟 30 次、同一 IP 每 10 分钟 5 次，超限返回 `code=429`。IP 提取优先取 `X-Forwarded-For` 首段，便于 Nginx 后部署。
