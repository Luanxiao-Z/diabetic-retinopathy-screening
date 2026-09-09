package cn.edu.fzu.drs.module.security.service;

import cn.edu.fzu.drs.module.security.model.AuthPrincipal;

/**
 * 令牌服务：本期基于 UUID Token + Redis（Session 模式）。
 */
public interface TokenService {

    /**
     * 创建令牌并写入 Redis（含角色派生的权限集与数据权限）。
     */
    String createToken(String userId, String username, String role);

    /**
     * 解析令牌对应的登录主体，失效返回 null。
     */
    AuthPrincipal getPrincipal(String token);

    /**
     * 续期（滑动过期）。
     */
    void refresh(String token);

    /**
     * 退出登录。
     */
    void remove(String token);

    /**
     * 令牌是否存在（未过期）。
     */
    boolean exists(String token);
}
