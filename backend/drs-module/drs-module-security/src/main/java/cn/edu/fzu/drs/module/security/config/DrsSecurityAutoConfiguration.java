package cn.edu.fzu.drs.module.security.config;

import cn.edu.fzu.drs.module.security.interceptor.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全自动配置：注册功能权限拦截器（AuthFilter 以 {@code @Component} 形式由容器注册）。
 */
@Configuration
public class DrsSecurityAutoConfiguration implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    public DrsSecurityAutoConfiguration(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/common/health");
    }
}
