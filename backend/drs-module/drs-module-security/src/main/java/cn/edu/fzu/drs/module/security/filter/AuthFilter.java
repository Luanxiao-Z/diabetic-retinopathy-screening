package cn.edu.fzu.drs.module.security.filter;

import cn.edu.fzu.drs.module.common.constant.AuthConstants;
import cn.edu.fzu.drs.module.common.exception.UnauthorizedException;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 认证过滤器：校验 X-Access-Token，将登录主体写入 {@link AuthContext}。
 * <p>匿名路径（auth 前缀、健康检查、文档）直接放行；其余路径缺令牌或失效则抛 401。</p>
 */
@Component
@Order(1)
public class AuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public AuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPermitAll(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader(AuthConstants.TOKEN_HEADER);
        if (token == null || !tokenService.exists(token)) {
            throw new UnauthorizedException();
        }
        AuthPrincipal principal = tokenService.getPrincipal(token);
        if (principal == null) {
            throw new UnauthorizedException();
        }
        AuthContext.set(principal);
        tokenService.refresh(token);
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private boolean isPermitAll(String path) {
        if (path.startsWith(AuthConstants.ANONYMOUS_PREFIX)) {
            return true;
        }
        if ("/api/v1/common/health".equals(path)) {
            return true;
        }
        if (path.startsWith("/doc.html") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return true;
        }
        return false;
    }
}
