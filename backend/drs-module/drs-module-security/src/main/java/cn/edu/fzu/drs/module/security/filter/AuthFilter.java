package cn.edu.fzu.drs.module.security.filter;

import cn.edu.fzu.drs.module.common.constant.AuthConstants;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.security.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 认证过滤器：校验 X-Access-Token，将登录主体写入 {@link AuthContext}。
 * <p>匿名路径（auth 前缀、健康检查、文档）直接放行；其余路径缺令牌或失效则直接写 JSON 401。</p>
 * <p>注意：Filter 层抛出的异常不会被 {@code @RestControllerAdvice} 捕获，因此 401 响应由本过滤器直接写出。</p>
 */
@Component
@Order(1)
public class AuthFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
            writeJson(response, AuthConstants.CODE_UNAUTHORIZED, "未登录或登录已过期");
            return;
        }
        AuthPrincipal principal = tokenService.getPrincipal(token);
        if (principal == null) {
            writeJson(response, AuthConstants.CODE_UNAUTHORIZED, "未登录或登录已过期");
            return;
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

    private void writeJson(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), Map.of("code", code, "msg", msg));
    }
}
