package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.system.service.CaptchaService;
import cn.edu.fzu.drs.module.system.vo.CaptchaVO;
import com.wf.captcha.SpecCaptcha;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

/**
 * 图形验证码实现：easy-captcha 生成 → Redis 存储 → 一次性消费。
 * <p>验证码文本只存在服务端，前端仅持有标识（captchaKey），因此无法在前端伪造校验。</p>
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final String KEY_PREFIX = "drs:auth:captcha:";

    /** 有效期（秒）：足够填写，又避免长期可重放 */
    private static final int TTL_SECONDS = 300;

    private static final int WIDTH = 130;
    private static final int HEIGHT = 44;
    private static final int LENGTH = 4;

    private final JedisPooled jedis;

    public CaptchaServiceImpl(JedisPooled jedis) {
        this.jedis = jedis;
    }

    @Override
    public CaptchaVO generate() {
        SpecCaptcha captcha = new SpecCaptcha(WIDTH, HEIGHT, LENGTH);
        String key = IdGenerator.nextId();
        // 统一按小写存储，校验时忽略大小写，减少用户因大小写输错的挫败感
        jedis.setex(KEY_PREFIX + key, TTL_SECONDS, captcha.text().toLowerCase());
        return new CaptchaVO(key, captcha.toBase64());
    }

    @Override
    public void verify(String key, String code) {
        if (key == null || key.isBlank()) {
            throw new BusinessException(400, "验证码已失效，请点击图片刷新后重试");
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "请输入验证码");
        }
        String redisKey = KEY_PREFIX + key;
        String expected = jedis.get(redisKey);
        // 一次性消费：无论校验是否通过都立即删除，防止同一验证码被暴力重试
        jedis.del(redisKey);
        if (expected == null) {
            throw new BusinessException(400, "验证码已过期，请点击图片刷新后重试");
        }
        if (!expected.equalsIgnoreCase(code.trim())) {
            throw new BusinessException(400, "验证码错误");
        }
    }
}
