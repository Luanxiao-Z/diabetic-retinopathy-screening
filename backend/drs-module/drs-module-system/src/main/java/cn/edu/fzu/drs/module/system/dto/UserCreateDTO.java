package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增用户入参。
 */
@Data
@Schema(description = "新增用户请求")
public class UserCreateDTO {

    @Schema(description = "登录用户名（唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @Schema(description = "真实姓名")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String realName;

    @Schema(description = "角色 DOCTOR/ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "DOCTOR|ADMIN", message = "角色仅支持 DOCTOR/ADMIN")
    private String role;

    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "状态 ENABLED/DISABLED，默认 ENABLED")
    private String status;

    @Schema(description = "初始密码（明文，存储时加密）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64")
    private String password;
}
