package cn.edu.fzu.drs.module.system.service;

import cn.edu.fzu.drs.module.system.vo.CaptchaVO;

/**
 * 图形验证码服务：生成与校验（一次性消费）。
 */
public interface CaptchaService {

    /**
     * 生成一张图形验证码，文本存 Redis，返回标识与图片。
     */
    CaptchaVO generate();

    /**
     * 校验验证码并立即失效（无论成功与否）。
     *
     * @param key  验证码标识
     * @param code 用户输入
     */
    void verify(String key, String code);
}
