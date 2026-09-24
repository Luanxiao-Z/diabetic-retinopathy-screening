package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改当前登录账号密码。
 */
public class PasswordChangeDTO {

    @Schema(description = "原密码")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "新密码长度需为 6-64")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
