package cn.edu.fzu.drs.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 自助注册入参。
 * <p>注册仅开放普通医生角色（DOCTOR，数据范围 SELF），管理员账号只能由既有管理员创建。</p>
 */
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$",
            message = "用户名需为 4-20 位，以字母开头，仅可包含字母、数字与下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,64}$",
            message = "密码需为 8-64 位，且同时包含字母与数字")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "真实姓名最长 20 个字符")
    private String realName;

    /** 选填；留空或符合大陆手机号格式 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
