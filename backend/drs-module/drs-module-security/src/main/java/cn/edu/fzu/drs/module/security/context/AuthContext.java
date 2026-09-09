package cn.edu.fzu.drs.module.security.context;

import cn.edu.fzu.drs.module.security.model.AuthPrincipal;

/**
 * 登录主体线程上下文：AuthFilter 写入、PermissionInterceptor / Service 读取，请求结束清除。
 */
public final class AuthContext {

    private static final ThreadLocal<AuthPrincipal> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthPrincipal principal) {
        HOLDER.set(principal);
    }

    public static AuthPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
