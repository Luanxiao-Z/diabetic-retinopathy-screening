package cn.edu.fzu.drs.module.common.constant;

/**
 * 认证与权限相关常量（前后端共用语义）。
 */
public final class AuthConstants {

    /** 令牌请求头 */
    public static final String TOKEN_HEADER = "X-Access-Token";

    /** 匿名接口前缀（无需登录） */
    public static final String ANONYMOUS_PREFIX = "/api/v1/auth/";

    /** 未登录 */
    public static final int CODE_UNAUTHORIZED = 401;

    /** 无权限 */
    public static final int CODE_FORBIDDEN = 403;

    private AuthConstants() {
    }
}
