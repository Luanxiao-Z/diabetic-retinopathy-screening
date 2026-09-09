package cn.edu.fzu.drs.module.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * Redis 连接配置（Jedis 连接池）。连接参数取自 spring.data.redis.*。
 * 注意：Docker 容器内 Redis（fjzhmz-redis）需密码认证（requirepass），
 * 必须显式传入密码，否则连接成功但执行命令时会被 NOAUTH 拒绝。
 */
@Configuration
public class RedisConfig {

    @Bean
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password) {
        if (password == null || password.isEmpty()) {
            return new JedisPooled(host, port);
        }
        // user 传 null：仅使用密码认证（适配 requirepass 模式），避免误发 ACL 双参 AUTH
        return new JedisPooled(host, port, null, password);
    }
}
