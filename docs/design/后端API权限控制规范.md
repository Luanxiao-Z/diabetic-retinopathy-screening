# 后端API权限控制规范

> **阅读约定**：本文中的 `{app}`、`{根包}`、`{根包路径}` 为占位符，含义与替换方式见[附录 13.1](#十三附录占位符与前后端约定)。
> **配套规范**：前端页面级与元素级权限控制见[前端页面权限控制规范](前端页面权限控制规范.md)；接口响应结构见[后端应用开发规范](../backend/后端应用开发规范.md)。

## 一、背景与目标

基于项目技术栈，在已有的登录认证方案基础上，设计一套完整的、基于URL路径前缀区分的API权限控制方案。不引入Spring Security，使用自定义Filter链实现。

需要覆盖以下7种场景：

1. **未登录可访问** — 登录相关接口
2. **仅校验登录令牌** — 字典明细批量查询、当前用户权限查询
3. **管理类API** — 校验登录令牌 + 校验Restful服务`@RequirePermission`注解定义的权限编码
4. **业务类API** — 校验登录令牌 + 校验`@RequirePermission`注解 + 业务逻辑中数据权限校验
5. **对外服务类API** — 不校验登录令牌，校验请求头中的服务签名
6. **Referer校验API** — 仅特定Referer请求头可访问
7. **IP白名单API** — 仅特定客户端IP可访问

***

## 二、核心架构设计

### 2.1 Filter链架构

采用**多Filter链式处理**模式，通过URL路径前缀匹配，将请求路由到不同的认证/鉴权策略。Filter链执行顺序由`FilterRegistrationBean`的`order`控制。

```
请求 → SecurityFilterChain
         │
         ├── [1] IpWhitelistFilter         — IP白名单校验（最高优先级，匹配即放行/拒绝）
         │
         ├── [2] RefererValidationFilter    — Referer校验（匹配路径则校验）
         │
         ├── [3] ServiceSignatureFilter     — 对外服务签名校验
         │
         ├── [4] AuthFilter                 — 登录令牌认证
         │
         └── [5] PermissionFilter           — 功能权限校验（@RequirePermission注解）
                  │
                  ▼
              Controller
                  │
                  ▼
              Service层（数据权限校验）
```

**处理逻辑**：

- 每个Filter通过`UrlPatternMatcher`判断当前请求URL是否属于自己的责任范围
- 命中的Filter执行校验逻辑，不命中的直接`chain.doFilter()`放行给下一个Filter
- Filter之间通过`HttpServletRequest.setAttribute()`传递认证结果

### 2.2 模块归属

安全相关功能归属于新增模块 `{app}-module-security`，在已有 `{app}-module-auth` 基础上扩展：

```
backend/{app}-module/{app}-module-security/
├── {app}-security-common/          # 共用代码：Filter、注解、UrlMatcher、配置
├── {app}-security-api/             # API接口声明（签名验证策略等）
└── {app}-security-service/         # Service实现（签名验证、数据权限等）
```

`{app}-module-auth` 保留现有登录认证功能，`{app}-module-security` 新增权限控制和各类校验功能。`{app}-security-common` 中的Filter可通过Spring Boot自动配置在各应用中生效。

***

## 三、URL路径前缀分配规则

### 3.1 路径规范设计

路径格式为 `/api/v1/{安全场景前缀}/{资源路径}`。**安全场景前缀**用于 Filter 链判定安全策略（即本节表格）；
其后的**资源路径**必须遵循 RESTful 规范——只含资源名词（复数、小写、连字符），禁止任何操作动词或 RPC 风格动作名，
操作一律由 HTTP 方法（`GET`/`POST`/`PUT`/`PATCH`/`DELETE`）表达。详见[后端应用开发规范](../backend/后端应用开发规范.md)。

| 前缀                         | 场景            | 校验策略                               | 说明                |
| -------------------------- | ------------- | ---------------------------------- | ----------------- |
| `/api/v1/auth/**`          | 场景1：未登录可访问    | `ANONYMOUS`                        | 会话（`/sessions`）、验证码（`/captchas`）等资源 |
| `/api/v1/common/**`        | 场景2：仅校验登录令牌   | `AUTHENTICATED`                    | 字典项（`/dicts/{domainCode}/items`）、当前用户权限（`/users/me/permissions`）等 |
| `/api/v1/admin/**`         | 场景3：管理类API    | `PERMISSION_REQUIRED`              | 用户（`/users`）、角色（`/roles`）、权限（`/permissions`）等管理类资源 |
| `/api/v1/biz/**`           | 场景4：业务类API    | `PERMISSION_REQUIRED + DATA_SCOPE` | 业务资源（如 `/orders`、`/screening-records`，需数据权限） |
| `/api/v1/open/**`          | 场景5：对外服务API   | `SERVICE_SIGNATURE`                | 对外同步资源（如 `PUT /users` 全量同步用户） |
| `/api/v1/config/**`        | 场景6：Referer校验 | `REFERER_CHECK`                    | 配置项资源（`/properties`）查询 |
| `/knife4j/**`, `/druid/**` | 场景7：IP白名单     | `IP_WHITELIST`                     | knife4j和druid监控页面 |

### 3.2 配置化路径规则

所有路径规则通过配置项管理，支持动态扩展：

```yaml
{app}:
  security:
    # URL路径权限规则配置
    url-rules:
      # 场景1：匿名访问
      - pattern: /api/v1/auth/**
        security-type: ANONYMOUS
        order: 1
      # 场景2：仅认证
      - pattern: /api/v1/common/**
        security-type: AUTHENTICATED
        order: 2
      # 场景3：认证+功能权限
      - pattern: /api/v1/admin/**
        security-type: PERMISSION_REQUIRED
        order: 3
      # 场景4：认证+功能权限+数据权限
      - pattern: /api/v1/biz/**
        security-type: PERMISSION_WITH_DATA_SCOPE
        order: 4
      # 场景5：对外服务签名
      - pattern: /api/v1/open/**
        security-type: SERVICE_SIGNATURE
        order: 5
      # 场景6：Referer校验
      - pattern: /api/v1/config/**
        security-type: REFERER_CHECK
        order: 6
      # 场景7：IP白名单
      - pattern: /knife4j/**
        security-type: IP_WHITELIST
        order: 7
      - pattern: /druid/**
        security-type: IP_WHITELIST
        order: 8

    # IP白名单配置
    ip-whitelist:
      paths:
        /knife4j/**:
          - 127.0.0.1
          - 10.0.0.0/8
          - 192.168.0.0/16
        /druid/**:
          - 127.0.0.1
          - 10.0.0.0/8
          - 192.168.0.0/16

    # Referer白名单配置
    referer-whitelist:
      /api/v1/config/**:
        - "https://admin.example.com"
        - "https://config.example.com"

    # 服务签名配置
    service-signature:
      header-name: X-Service-Signature
      header-app-key: X-App-Key
      header-timestamp: X-Timestamp
      header-nonce: X-Nonce
      timestamp-valid-seconds: 300
      apps:
        - app-key: sync-service-001
          app-secret: ${SYNC_SERVICE_SECRET:}   # 从环境变量获取
        - app-key: data-export-service-001
          app-secret: ${DATA_EXPORT_SECRET:}
```

***

## 四、详细设计

### 4.1 SecurityType安全类型常量

```java
package {根包}.security.common.constant;

public final class SecurityType {
    private SecurityType() {}

    /** 匿名访问 — 不校验登录令牌，直接放行 */
    public static final String ANONYMOUS = "ANONYMOUS";
    /** 仅认证 — 只校验登录令牌有效性 */
    public static final String AUTHENTICATED = "AUTHENTICATED";
    /** 功能权限 — 校验登录令牌 + @RequirePermission注解权限 */
    public static final String PERMISSION_REQUIRED = "PERMISSION_REQUIRED";
    /** 功能权限+数据权限 — 校验登录令牌 + @RequirePermission + 业务层数据权限 */
    public static final String PERMISSION_WITH_DATA_SCOPE = "PERMISSION_WITH_DATA_SCOPE";
    /** 服务签名 — 不校验登录令牌，校验请求头中的服务签名 */
    public static final String SERVICE_SIGNATURE = "SERVICE_SIGNATURE";
    /** Referer校验 — 校验请求头Referer是否在白名单内 */
    public static final String REFERER_CHECK = "REFERER_CHECK";
    /** IP白名单 — 仅允许指定IP访问 */
    public static final String IP_WHITELIST = "IP_WHITELIST";

    /**
     * 校验给定的安全类型字符串是否合法
     */
    public static boolean isValid(String securityType) {
        return ANONYMOUS.equals(securityType)
            || AUTHENTICATED.equals(securityType)
            || PERMISSION_REQUIRED.equals(securityType)
            || PERMISSION_WITH_DATA_SCOPE.equals(securityType)
            || SERVICE_SIGNATURE.equals(securityType)
            || REFERER_CHECK.equals(securityType)
            || IP_WHITELIST.equals(securityType);
    }
}
```

### 4.2 URL路径规则模型

```java
package {根包}.security.common.model;

public class UrlSecurityRule {
    /** URL匹配模式（Ant风格） */
    private String pattern;
    /** 安全类型，取值见 SecurityType 常量类 */
    private String securityType;
    /** 匹配优先级（越小越优先） */
    private int order;
}
```

### 4.3 UrlPatternMatcher（URL匹配器）

```java
package {根包}.security.common.matcher;

public class UrlPatternMatcher {
    private final List<UrlSecurityRule> rules;

    /**
     * 根据请求路径匹配对应的安全规则
     * 按order升序匹配，返回第一个命中的规则
     */
    public UrlSecurityRule match(String requestUri);

    /**
     * 判断请求路径是否属于指定安全类型
     */
    public boolean isType(String requestUri, String securityType);
}
```

***

## 五、各Filter详细设计

### 5.1 IpWhitelistFilter（场景7：IP白名单）

**职责**：对配置了IP白名单的路径，仅允许白名单内的客户端IP访问。

**处理流程**：

1. 从配置中获取需要IP白名单校验的路径规则
2. 判断当前请求路径是否命中IP白名单规则
3. 若命中，获取客户端真实IP（考虑代理：优先`X-Forwarded-For`、`X-Real-IP`）
4. 校验IP是否在白名单内（支持CIDR网段格式）
5. 不在白名单 → 返回`403 Forbidden`
6. 在白名单 → `chain.doFilter()` 跳过后续所有认证Filter，直接到达目标资源

**关键实现点**：

- IP匹配支持精确IP和CIDR网段（如`192.168.0.0/16`）
- 需要处理代理场景下的真实IP获取
- 命中IP白名单后，后续Filter应自动跳过（通过在request中设置标记属性）

```java
package {根包}.security.common.filter;

public class IpWhitelistFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        // 1. 获取请求URI
        // 2. 匹配IP白名单规则
        // 3. 若命中，校验客户端IP
        // 4. 命中且IP合法 → 标记BYPASS_ALL，直接放行
        // 5. 未命中规则 → 放行给下一个Filter
        // 6. 命中但IP非法 → 403
    }
}
```

### 5.2 RefererValidationFilter（场景6：Referer校验）

**职责**：对需要Referer校验的路径，检查请求头`Referer`是否在白名单内。

**处理流程**：

1. 从配置中获取Referer白名单规则
2. 判断当前请求路径是否命中Referer校验规则
3. 若命中，获取请求头`Referer`的值
4. 与配置的白名单进行匹配（支持精确匹配和通配符）
5. 不匹配 → 返回`403 Forbidden`
6. 匹配 → `chain.doFilter()`

**关键实现点**：

- 允许配置多个合法Referer
- 支持通配符匹配（如`*.example.com`）
- 若客户端未传Referer，视为非法

### 5.3 ServiceSignatureFilter（场景5：对外服务签名校验）

**职责**：对对外暴露的API，校验请求头中的服务签名，确保调用方身份合法。

**处理流程**：

1. 判断当前请求路径是否命中`SERVICE_SIGNATURE`规则
2. 若命中，从请求头获取：`X-App-Key`、`X-Timestamp`、`X-Nonce`、`X-Service-Signature`
3. 校验时间戳是否在有效期内（默认5分钟，防重放攻击）
4. 校验Nonce是否已使用（Redis缓存防重放，缓存TTL=时间戳有效时长）
5. 根据`X-App-Key`查找对应的`app-secret`
6. 计算签名：`HMAC-SHA256(app-secret, body + timestamp + nonce)`
7. 比对签名是否一致
8. 签名不合法 → 返回`401 Unauthorized` + 错误信息
9. 签名合法 → `chain.doFilter()`

**签名算法**：

```
signature = Base64(HMAC-SHA256(app-secret, requestBody + "|" + timestamp + "|" + nonce))
```

**关键实现点**：

- 请求体需要可重复读取（使用`ContentCachingRequestWrapper`）
- Nonce通过Redis缓存防重放，key为`security:nonce:{nonce}`，TTL为时间戳有效时长
- 支持多个App-Key/App-Secret对，通过配置管理
- 签名校验策略接口化，方便扩展：

```java
package {根包}.security.api.strategy;

public interface SignatureVerifyStrategy {
    boolean verify(String appKey, String signature, String body, 
                   long timestamp, String nonce);
    String getAppSecret(String appKey);
}
```

### 5.4 AuthFilter（场景1/2/3/4：登录令牌认证）

**职责**：对需要登录认证的API，校验请求头中的登录令牌（`X-Access-Token`）有效性。

**改进点**（在现有AuthFilter基础上扩展）：

1. 通过`UrlPatternMatcher`判断当前请求是否为`ANONYMOUS`类型 → 直接放行
2. 通过`UrlPatternMatcher`判断当前请求是否为`SERVICE_SIGNATURE`类型 → 直接放行（由ServiceSignatureFilter处理）
3. 通过`UrlPatternMatcher`判断当前请求是否需要`IP_WHITELIST`类型 → 若已标记BYPASS则放行
4. 其余请求执行原有登录令牌校验逻辑：
   - 从请求头`X-Access-Token`获取令牌
   - 从Redis缓存获取`LoginCacheInfo`
   - 校验令牌有效性和过期时间
   - 将`LoginCacheInfo`存入`HttpServletRequest.setAttribute()`
   - 检查缓存续期

**排除路径**（由配置管理）：

```yaml
{app}:
  auth:
    exclude-paths:
      - /api/v1/auth/captchas/**
      - /api/v1/auth/sessions/**
      - /api/v1/open/**
      - /knife4j/**
      - /druid/**
```

### 5.5 PermissionFilter（场景3/4：功能权限校验）

**职责**：对需要功能权限校验的API，读取Controller方法上的`@RequirePermission`注解并校验。

**处理流程**：

1. 通过`UrlPatternMatcher`判断当前请求是否为`PERMISSION_REQUIRED`或`PERMISSION_WITH_DATA_SCOPE`类型
2. 若不匹配 → 直接放行
3. 若匹配，从`HandlerMapping`获取目标方法上的`@RequirePermission`注解
4. 若无注解 → 放行
5. 若有注解，获取当前用户权限列表（通过`PermissionQueryStrategy`）
6. 校验是否满足注解中定义的权限要求（支持AND/OR逻辑）
7. 不满足 → 返回`403 Forbidden` + `{code:403, msg:"无权限访问"}`
8. 满足 → 放行，若为`PERMISSION_WITH_DATA_SCOPE`类型，在request中标记`NEED_DATA_SCOPE`

### 5.6 @RequirePermission注解设计

```java
package {根包}.security.common.annotation;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /** 所需权限编码 */
    String[] value();
    /** 权限校验逻辑："AND"=需全部满足（默认），"OR"=满足任一即可 */
    String logical() default "AND";
    /** 权限类型："FUNCTION"=功能权限（默认），"DATA"=数据权限 */
    String type() default "FUNCTION";
}
```

对应的常量类：

```java
package {根包}.security.common.constant;

public final class PermissionLogical {
    private PermissionLogical() {}

    /** 需全部满足 */
    public static final String AND = "AND";
    /** 满足任一即可 */
    public static final String OR = "OR";
}

public final class PermissionType {
    private PermissionType() {}

    /** 功能权限 */
    public static final String FUNCTION = "FUNCTION";
    /** 数据权限 */
    public static final String DATA = "DATA";
}
```

**使用示例**：

```java
// 场景3：管理类 — 仅功能权限
// 说明：新增用户属于主体数据修改，按权限精简原则归入 sys:user:edit，不单独定义 sys:user:add
// 路径为资源名词复数，操作由 POST 表达
@PostMapping("/api/v1/admin/users")
@RequirePermission("sys:user:edit")
public Result createUser(@RequestBody UserCreateDTO dto) { ... }

// 场景4：业务类 — 功能权限 + 业务逻辑中数据权限
// 查询使用 GET，参数通过查询参数绑定
@GetMapping("/api/v1/biz/orders")
@RequirePermission("biz:order:list")
public Result pageOrders(OrderQueryDTO query) {
    // 在Service层调用数据权限校验
    DataScope dataScope = DataScopeUtils.getCurrentDataScope();
    query.setRegionIds(dataScope.getRegionIds());
    return orderService.page(query);
}
```

***

## 六、数据权限校验设计（场景4核心）

### 6.1 数据权限模型

```java
package {根包}.security.common.constant;

public final class DataScopeType {
    private DataScopeType() {}

    /** 全部数据权限 */
    public static final String ALL = "ALL";
    /** 本部门 */
    public static final String DEPT = "DEPT";
    /** 本部门及子部门 */
    public static final String DEPT_AND_CHILD = "DEPT_AND_CHILD";
    /** 指定区域 */
    public static final String REGION = "REGION";
    /** 自定义 */
    public static final String CUSTOM = "CUSTOM";
    /** 仅本人 */
    public static final String SELF = "SELF";
}
```

```java
package {根包}.security.common.model;

public class DataScope {
    /** 数据权限类型，取值见 DataScopeType 常量类 */
    private String scopeType;
    /** 有权限的组织/部门ID列表 */
    private Set<String> deptIds;
    /** 有权限的区域ID列表 */
    private Set<String> regionIds;
    /** 自定义SQL片段 */
    private String customSql;
}
```

### 6.2 DataScopeUtils工具类

```java
package {根包}.security.common.util;

public class DataScopeUtils {
    /**
     * 获取当前用户的数据权限范围
     * 通过DataScopeQueryStrategy策略查询
     * 未登录抛出AuthException
     */
    public static DataScope getCurrentDataScope();
    
    /**
     * 获取当前用户在某业务模块的数据权限范围
     * @param moduleCode 业务模块编码
     */
    public static DataScope getDataScope(String moduleCode);
}
```

### 6.3 DataScopeQueryStrategy策略接口

```java
package {根包}.security.api.strategy;

public interface DataScopeQueryStrategy {
    /**
     * 查询当前用户的数据权限范围
     * @param userId 用户ID
     * @param moduleCode 业务模块编码（可选）
     */
    DataScope queryDataScope(String userId, String moduleCode);
}
```

### 6.4 数据权限在MyBatis-Plus中的集成

业务模块在MyBatis-Plus查询中通过**SQL拦截器**或**自定义BaseMapper**注入数据权限条件：

```java
// 方式一：自定义BaseMapper扩展
public interface DataScopeMapper<T> extends BaseMapper<T> {
    default LambdaQueryWrapper<T> withDataScope() {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        DataScope dataScope = DataScopeUtils.getCurrentDataScope();
        if (DataScopeType.REGION.equals(dataScope.getScopeType())) {
            wrapper.in(entity -> entity.getRegionId(), dataScope.getRegionIds());
        } else if (DataScopeType.DEPT.equals(dataScope.getScopeType())) {
            wrapper.eq(entity -> entity.getDeptId(), ...);
        }
        // ... 其他类型处理
        return wrapper;
    }
}
```

***

## 七、异常处理与响应格式

### 7.1 统一异常处理

所有 Filter 校验失败时返回统一格式的 JSON 响应（下方示例中 `//` 为场景说明注释，实际响应中不存在）：

```jsonc
// 未登录 - 401
{
  "code": 401,
  "msg": "登录已过期，请重新登录"
}

// 无权限 - 403
{
  "code": 403,
  "msg": "无权限访问该资源"
}

// 签名校验失败 - 401
{
  "code": 401,
  "msg": "服务签名校验失败"
}

// Referer校验失败 - 403
{
  "code": 403,
  "msg": "非法的请求来源"
}

// IP校验失败 - 403
{
  "code": 403,
  "msg": "IP未被授权访问"
}
```

### 7.2 响应写入工具

```java
package {根包}.security.common.util;

public class SecurityResponseUtils {
    /**
     * 写入JSON格式的错误响应
     */
    public static void writeUnauthorized(HttpServletResponse response, String message);
    public static void writeForbidden(HttpServletResponse response, String message);
}
```

**实现要点**：

- 设置`Content-Type: application/json;charset=UTF-8`
- 使用Jackson序列化响应对象
- 对AJAX请求和普通请求做区分（AJAX返回JSON，页面请求可返回JSON或重定向）

***

## 八、Spring Boot自动配置

### 8.1 SecurityAutoConfiguration

```java
package {根包}.security.common.config;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "{app}.security", name = "enabled", 
                          havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<IpWhitelistFilter> ipWhitelistFilterRegistration() {
        FilterRegistrationBean<IpWhitelistFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IpWhitelistFilter(securityProperties));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("ipWhitelistFilter");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "{app}.security", name = "enabled", 
                          havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RefererValidationFilter> refererFilterRegistration() {
        FilterRegistrationBean<RefererValidationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RefererValidationFilter(securityProperties));
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        registration.setName("refererValidationFilter");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "{app}.security", name = "enabled", 
                          havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<ServiceSignatureFilter> signatureFilterRegistration() {
        FilterRegistrationBean<ServiceSignatureFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ServiceSignatureFilter(securityProperties, cacheService));
        registration.addUrlPatterns("/*");
        registration.setOrder(3);
        registration.setName("serviceSignatureFilter");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "{app}.security", name = "enabled", 
                          havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AuthFilter> authFilterRegistration() {
        FilterRegistrationBean<AuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthFilter(securityProperties, cacheService));
        registration.addUrlPatterns("/*");
        registration.setOrder(100);
        registration.setName("authFilter");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "{app}.security", name = "enabled", 
                          havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<PermissionFilter> permissionFilterRegistration() {
        FilterRegistrationBean<PermissionFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new PermissionFilter(securityProperties));
        registration.addUrlPatterns("/*");
        registration.setOrder(101);
        registration.setName("permissionFilter");
        return registration;
    }
}
```

### 8.2 SecurityProperties配置属性类

```java
package {根包}.security.common.config;

@ConfigurationProperties(prefix = "{app}.security")
public class SecurityProperties {
    /** 是否启用安全模块 */
    private boolean enabled = true;
    /** URL安全规则配置 */
    private List<UrlRuleConfig> urlRules = new ArrayList<>();
    /** IP白名单配置 */
    private Map<String, List<String>> ipWhitelist = new HashMap<>();
    /** Referer白名单配置 */
    private Map<String, List<String>> refererWhitelist = new HashMap<>();
    /** 服务签名配置 */
    private SignatureConfig serviceSignature = new SignatureConfig();

    public static class UrlRuleConfig {
        private String pattern;
        private SecurityType securityType;
        private int order;
        // getter/setter
    }

    public static class SignatureConfig {
        private String headerName = "X-Service-Signature";
        private String headerAppKey = "X-App-Key";
        private String headerTimestamp = "X-Timestamp";
        private String headerNonce = "X-Nonce";
        private long timestampValidSeconds = 300;
        private List<AppConfig> apps = new ArrayList<>();
        // getter/setter
    }

    public static class AppConfig {
        private String appKey;
        private String appSecret;
        // getter/setter
    }
}
```

### 8.3 spring.factories自动配置声明

```
# src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
{根包}.security.common.config.SecurityAutoConfiguration
```

***

## 九、模块包结构总览

```
backend/{app}-module/{app}-module-security/
└── {app}-security-common/
    └── src/main/java/{根包路径}/security/common/
        ├── annotation/
        │   └── RequirePermission.java          # 权限校验注解
        ├── config/
        │   ├── SecurityProperties.java          # 安全配置属性
        │   └── SecurityAutoConfiguration.java   # Filter自动配置
        ├── constant/
        │   ├── SecurityType.java               # 安全类型常量
        │   ├── DataScopeType.java              # 数据权限类型常量
        │   ├── PermissionLogical.java          # 权限校验逻辑常量
        │   └── PermissionType.java             # 权限类型常量
        ├── filter/
        │   ├── IpWhitelistFilter.java           # IP白名单Filter
        │   ├── RefererValidationFilter.java     # Referer校验Filter
        │   ├── ServiceSignatureFilter.java      # 服务签名Filter
        │   ├── AuthFilter.java                  # 登录认证Filter（改进现有）
        │   └── PermissionFilter.java            # 功能权限Filter（改进现有）
        ├── matcher/
        │   └── UrlPatternMatcher.java           # URL匹配器
        ├── model/
        │   ├── DataScope.java                  # 数据权限模型
        │   └── UrlSecurityRule.java            # URL安全规则模型
        └── util/
            ├── SecurityResponseUtils.java       # 安全响应写入工具
            ├── DataScopeUtils.java              # 数据权限工具类
            ├── IpUtils.java                     # IP校验工具（CIDR匹配）
            └── SignatureUtils.java             # 签名计算工具

backend/{app}-module/{app}-module-security/
└── {app}-security-api/
    └── src/main/java/{根包路径}/security/api/
        └── strategy/
            ├── SignatureVerifyStrategy.java    # 签名验证策略
            └── DataScopeQueryStrategy.java     # 数据权限查询策略

backend/{app}-module/{app}-module-security/
└── {app}-security-service/
    └── src/main/java/{根包路径}/security/service/
        └── impl/
            ├── DefaultSignatureVerifyService.java  # 默认签名验证实现
            └── DefaultDataScopeQueryService.java   # 默认数据权限查询实现
```

***

## 十、场景测试用例清单

### 10.1 场景1：未登录可访问

| 测试用例                                       | 预期结果         |
| ------------------------------------------ | ------------ |
| `POST /api/v1/auth/sessions` 不带token | 201，正常创建会话（登录） |
| `GET /api/v1/auth/captchas` 不带token | 200，正常返回验证码 |

### 10.2 场景2：仅校验登录令牌

| 测试用例                                          | 预期结果       |
| --------------------------------------------- | ---------- |
| `GET /api/v1/common/dicts/B_GENDER/items` 带有效token | 200，正常返回字典项 |
| `GET /api/v1/common/dicts/B_GENDER/items` 不带token | 401，登录已过期 |
| `GET /api/v1/common/dicts/B_GENDER/items` 带过期token | 401，登录已过期 |
| `GET /api/v1/common/users/me/permissions` 带有效token | 200，返回权限列表 |

### 10.3 场景3：管理类API（认证+功能权限）

| 测试用例                                                      | 预期结果    |
| --------------------------------------------------------- | ------- |
| `POST /api/v1/admin/users` 带有效token+拥有`sys:user:edit`权限 | 201     |
| `POST /api/v1/admin/users` 带有效token+无`sys:user:edit`权限 | 403，无权限 |
| `POST /api/v1/admin/users` 不带token | 401     |

### 10.4 场景4：业务类API（认证+功能权限+数据权限）

| 测试用例                                                                 | 预期结果         |
| -------------------------------------------------------------------- | ------------ |
| `GET /api/v1/biz/orders` 带有效token+拥有`biz:order:list`权限，查询A区域 | 200，仅返回区域A数据 |
| `GET /api/v1/biz/orders` 带有效token+无`biz:order:list`权限 | 403          |
| `GET /api/v1/biz/orders` 带有效token+拥有权限但数据权限仅限B区域，查询A区域 | 200，但返回空数据   |

### 10.5 场景5：对外服务API（签名校验）

| 测试用例                                          | 预期结果       |
| --------------------------------------------- | ---------- |
| `PUT /api/v1/open/users` 带合法App-Key+正确签名 | 200        |
| `PUT /api/v1/open/users` 带合法App-Key+错误签名 | 401，签名校验失败 |
| `PUT /api/v1/open/users` 不带签名 | 401        |
| `PUT /api/v1/open/users` 时间戳过期 | 401，签名校验失败 |

### 10.6 场景6：Referer校验

| 测试用例                                                          | 预期结果 |
| ------------------------------------------------------------- | ---- |
| `GET /api/v1/config/properties` Referer=<https://admin.example.com> | 200  |
| `GET /api/v1/config/properties` 不传Referer | 403  |
| `GET /api/v1/config/properties` Referer=<https://evil.com> | 403  |

### 10.7 场景7：IP白名单

| 测试用例                                    | 预期结果 |
| --------------------------------------- | ---- |
| `GET /knife4j/doc.html` 客户端IP=127.0.0.1 | 200  |
| `GET /druid/index.html` 客户端IP=10.0.5.1  | 200  |
| `GET /knife4j/doc.html` 客户端IP=8.8.8.8   | 403  |

***

## 十一、实施步骤

### 第1步：新增`{app}-module-security`模块骨架

- 创建`{app}-security-common`、`{app}-security-api`、`{app}-security-service`子模块
- 创建`pom.xml`，声明模块间依赖关系
- `{app}-security-common`依赖`{app}-auth-common`（复用AuthUtils、LoginCacheInfo等）

### 第2步：实现SecurityProperties配置类和SecurityType枚举

- `SecurityProperties`：配置属性绑定
- `SecurityType`：7种安全类型枚举
- `UrlSecurityRule`：URL规则模型

### 第3步：实现UrlPatternMatcher

- Ant路径模式匹配
- 按优先级排序的URL安全规则匹配

### 第4步：实现各Filter（按优先级顺序）

- **IpWhitelistFilter**：CIDR IP匹配、代理IP识别
- **RefererValidationFilter**：Referer匹配（支持通配符）
- **ServiceSignatureFilter**：HMAC-SHA256签名计算、时间戳校验、Nonce防重放
- **AuthFilter改进**：增加`UrlPatternMatcher`集成、IP标记放行
- **PermissionFilter改进**：增加`PERMISSION_WITH_DATA_SCOPE`类型处理

### 第5步：实现@RequirePermission注解

- 方法级和类级支持
- AND/OR逻辑支持
- PermissionType扩展（FUNCTION/DATA）

### 第6步：实现签名验证和数据权限策略接口

- `SignatureVerifyStrategy`接口 + 默认HMAC实现
- `DataScopeQueryStrategy`接口 + 默认实现

### 第7步：实现SecurityAutoConfiguration

- Filter的FilterRegistrationBean注册
- UrlPatternMatcher Bean定义
- spring.factories自动配置声明

### 第8步：实现工具类

- `SecurityResponseUtils`：JSON响应写入
- `DataScopeUtils`：数据权限获取
- `IpUtils`：CIDR IP匹配
- `SignatureUtils`：HMAC-SHA256签名

### 第9步：改进现有{app}-module-auth模块

- AuthFilter增加匿名路径和已标记放行的判断逻辑
- PermissionFilter增加SecurityType区分
- 将@RequirePermission注解移至{app}-security-common

### 第10步：编写单元测试

- 各Filter的单元测试（Mock request/response）
- UrlPatternMatcher匹配测试
- IpUtils CIDR匹配测试
- 签名计算和验证测试

### 第11步：编写集成测试

- 7种场景的端到端测试
- Filter链执行顺序验证

***

## 十二、风险与注意事项

1. **Filter执行顺序**：必须严格按照`IpWhitelistFilter(1) → RefererValidationFilter(2) → ServiceSignatureFilter(3) → AuthFilter(100) → PermissionFilter(101)`的顺序，否则会出现逻辑错误
2. **签名校验的Body读取**：需要使用`ContentCachingRequestWrapper`包装请求，确保Body可以被多次读取
3. **IP白名单的代理场景**：需正确处理`X-Forwarded-For`、`X-Real-IP`请求头，避免IP伪造
4. **Nonce防重放的Redis依赖**：若Redis不可用，签名校验可能误拒合法请求，需要合理的降级策略
5. **数据权限的性能**：大数据量场景下数据权限条件可能影响SQL执行计划，需要关注索引设计

---

## 十三、附录：占位符与前后端约定

### 13.1 占位符说明

本文中的占位符在实际落地时需全局替换为项目统一值：

| 占位符 | 含义 | 替换示例 |
| --- | --- | --- |
| `{app}` | 项目英文前缀，用于模块名与配置前缀 | `drs`（则模块为 `drs-module-security`，配置为 `drs.security`） |
| `{根包}` | Java 根包名 | `com.example.drs`（则包为 `com.example.drs.security.common`） |
| `{根包路径}` | Java 根包对应的目录路径 | `com/example/drs` |

### 13.2 RESTful 路径约定

本节所有示例路径均遵循 RESTful 规范：

| 传统 RPC 写法 | 本规范写法 | HTTP 方法 |
| --- | --- | --- |
| `/auth/login/password` | `/auth/sessions` | `POST` |
| `/auth/logout` | `/auth/sessions` | `DELETE` |
| `/auth/captcha/image` | `/auth/captchas` | `GET` |
| `/common/dict/list` | `/common/dicts/{domainCode}/items` | `GET` |
| `/common/user/permissions` | `/common/users/me/permissions` | `GET` |
| `/admin/user/add` | `/admin/users` | `POST` |
| `/biz/order/list` | `/biz/orders` | `GET` |
| `/open/user/sync` | `/open/users` | `PUT` |
| `/config/list` | `/config/properties` | `GET` |

Filter 链按**路径前缀**匹配安全策略，与资源路径的 RESTful 形式无关，因此前缀体系保持不变。

### 13.3 与前端权限编码的对齐约定

- 后端 `@RequirePermission` 的权限编码与前端路由/按钮权限编码**必须完全一致**，共用同一份编码清单。
- 权限编码遵循**精简原则**：对页面主体数据的增、删、改、禁、启等修改类操作合并为一个 `edit` 权限；只有操作不同的关联数据时才定义独立权限。详见[前端页面权限控制规范](前端页面权限控制规范.md)。
- 因此本文中示例使用 `sys:user:edit`（新增用户属于主体数据修改，归入 `edit`），而非 `sys:user:add`。
- 前端不得定义后端不存在的权限编码；后端新增权限编码时需同步更新前端权限清单。
