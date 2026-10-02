package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;

/**
 * 轻量限流器（基于 Redis 固定窗口计数）。
 *
 * <p>用于自助注册等匿名写接口，防止批量刷号。采用**双层**策略：</p>
 * <ul>
 *   <li>全局层：限制全站总请求量，防御分布式来源的刷号；</li>
 *   <li>IP 层：限制单一来源，避免个别客户端占满全局配额。</li>
 * </ul>
 *
 * <p>计数采用「先自增、首次设置过期」的固定窗口实现；incr 与 expire 之间存在极小竞态窗口
 * （进程在两者之间崩溃会导致该 key 不过期），对本场景可接受。</p>
 */
@Service
public class RateLimiter {

    /** 全局窗口：每分钟允许的注册请求上限 */
    private static final int GLOBAL_LIMIT = 30;
    private static final int GLOBAL_WINDOW_SECONDS = 60;

    /** IP 窗口：每 10 分钟允许的注册请求上限 */
    private static final int IP_LIMIT = 5;
    private static final int IP_WINDOW_SECONDS = 600;

    private final JedisPooled jedis;

    public RateLimiter(JedisPooled jedis) {
        this.jedis = jedis;
    }

    /**
     * 校验注册请求是否放行，超限抛出 429。
     *
     * @param ip 客户端 IP（为空时跳过 IP 维度，仅受全局层约束）
     */
    public void checkRegister(String ip) {
        String minuteKey = "drs:auth:register:global:" + (System.currentTimeMillis() / 60000);
        long globalCount = increment(minuteKey, GLOBAL_WINDOW_SECONDS);
        if (globalCount > GLOBAL_LIMIT) {
            throw new BusinessException(429, "系统繁忙，请稍后再试");
        }

        if (ip == null || ip.isBlank()) {
            return;
        }
        String ipKey = "drs:auth:register:ip:" + ip;
        long ipCount = increment(ipKey, IP_WINDOW_SECONDS);
        if (ipCount > IP_LIMIT) {
            throw new BusinessException(429, "注册过于频繁，请 " + formatWait(IP_WINDOW_SECONDS) + "后再试");
        }
    }

    private long increment(String key, int ttlSeconds) {
        Long count = jedis.incr(key);
        if (count == null) {
            return 0L;
        }
        if (count == 1L) {
            jedis.expire(key, ttlSeconds);
        }
        return count;
    }

    private String formatWait(int seconds) {
        Duration d = Duration.ofSeconds(seconds);
        long minutes = d.toMinutes();
        return minutes > 0 ? minutes + " 分钟" : seconds + " 秒";
    }
}
