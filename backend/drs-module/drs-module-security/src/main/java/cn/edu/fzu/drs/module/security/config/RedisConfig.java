package cn.edu.fzu.drs.module.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * Redis 连接配置（Jedis 连接池）。连接参数取自 spring.data.redis.*。
 */
@Configuration
public class RedisConfig {

    @Bean
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return new JedisPooled(host, port);
    }
}
