package cn.edu.fzu.drs.module.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 登录主体：从 Redis 令牌反序列化得到，贯穿一次请求生命周期（存于 {@link cn.edu.fzu.drs.module.security.context.AuthContext}）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthPrincipal {

    private String userId;
    private String username;
    private String role;
    private Set<String> permissions;
    private String dataScope;
}
