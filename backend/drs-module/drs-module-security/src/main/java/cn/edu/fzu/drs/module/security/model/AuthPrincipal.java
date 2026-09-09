package cn.edu.fzu.drs.module.security.model;

import java.util.Set;

/**
 * 登录主体：从 Redis 令牌反序列化得到，贯穿一次请求生命周期（存于 {@link cn.edu.fzu.drs.module.security.context.AuthContext}）。
 */
public class AuthPrincipal {

    private String userId;
    private String username;
    private String role;
    private Set<String> permissions;
    private String dataScope;

    public AuthPrincipal() {
    }

    public AuthPrincipal(String userId, String username, String role, Set<String> permissions, String dataScope) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.permissions = permissions;
        this.dataScope = dataScope;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }
}
