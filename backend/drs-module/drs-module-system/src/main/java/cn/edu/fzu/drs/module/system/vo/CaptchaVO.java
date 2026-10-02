package cn.edu.fzu.drs.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码出参（GET /api/v1/auth/captcha）。
 * <p>验证码文本存于服务端 Redis，此处仅下发标识与图片，避免明文暴露。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码标识，登录时需一并回传 */
    private String captchaKey;

    /** 图片 Base64（含 data URI 前缀，可直接用于 img src） */
    private String image;
}
