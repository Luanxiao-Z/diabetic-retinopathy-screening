package cn.edu.fzu.drs.module.security.service.impl;

import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.security.core.PermissionResolver;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.security.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

/**
 * 令牌服务 Redis 实现：令牌为 UUID，value 为登录主体的 JSON，TTL 2 小时，访问即续期。
 */
@Service
public class RedisTokenServiceImpl implements TokenService {

    private static final String KEY_PREFIX = "drs:auth:token:";
    private static final int TTL_SECONDS = 7200;

    private final JedisPooled jedis;
    private final PermissionResolver permissionResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisTokenServiceImpl(JedisPooled jedis, PermissionResolver permissionResolver) {
        this.jedis = jedis;
        this.permissionResolver = permissionResolver;
    }

    @Override
    public String createToken(String userId, String username, String role) {
        String token = IdGenerator.nextId();
        AuthPrincipal principal = new AuthPrincipal(userId, username, role,
                permissionResolver.resolve(role), permissionResolver.dataScope(role));
        try {
            jedis.setex(KEY_PREFIX + token, TTL_SECONDS, objectMapper.writeValueAsString(principal));
        } catch (Exception e) {
            throw new IllegalStateException("令牌写入 Redis 失败", e);
        }
        return token;
    }

    @Override
    public AuthPrincipal getPrincipal(String token) {
        String json = jedis.get(KEY_PREFIX + token);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, AuthPrincipal.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void refresh(String token) {
        jedis.expire(KEY_PREFIX + token, TTL_SECONDS);
    }

    @Override
    public void remove(String token) {
        jedis.del(KEY_PREFIX + token);
    }

    @Override
    public boolean exists(String token) {
        return jedis.exists(KEY_PREFIX + token);
    }
}
