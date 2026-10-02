package cn.edu.fzu.drs.module.system.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录入参。
 */
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 验证码标识（由 GET /auth/captcha 下发） */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaKey;

    /** 用户输入的验证码 */
    @NotBlank(message = "验证码不能为空")
    private String captchaCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
