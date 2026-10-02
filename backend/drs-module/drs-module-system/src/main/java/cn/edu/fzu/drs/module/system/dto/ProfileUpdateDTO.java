package cn.edu.fzu.drs.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 当前用户资料修改入参（PUT /api/v1/common/users/me）。
 *
 * <p><b>仅开放真实姓名与手机号</b>：角色、状态、数据范围等权限相关字段不可由本人变更，
 * 否则等于绕过权限体系自我提权。</p>
 */
public class ProfileUpdateDTO {

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 20, message = "真实姓名最长 20 个字符")
    private String realName;

    /** 选填；留空或符合大陆手机号格式 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

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
