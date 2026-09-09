package cn.edu.fzu.drs.module.security.interceptor;

import cn.edu.fzu.drs.module.common.exception.AccessDeniedException;
import cn.edu.fzu.drs.module.common.exception.UnauthorizedException;
import cn.edu.fzu.drs.module.security.annotation.RequirePermission;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 功能权限拦截器：读取方法/类上的 {@link RequirePermission}，校验当前登录主体是否持有该权限编码。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            annotation = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (annotation == null) {
            return true;
        }
        var principal = AuthContext.get();
        if (principal == null) {
            throw new UnauthorizedException();
        }
        if (!principal.getPermissions().contains(annotation.value())) {
            throw new AccessDeniedException();
        }
        return true;
    }
}
