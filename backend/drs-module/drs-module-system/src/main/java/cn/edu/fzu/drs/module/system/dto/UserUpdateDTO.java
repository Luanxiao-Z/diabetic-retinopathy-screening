package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户入参（全量更新，未提供字段不覆盖）。
 */
@Data
@Schema(description = "修改用户请求")
public class UserUpdateDTO {

    @Schema(description = "真实姓名")
    @Size(max = 64, message = "姓名长度不能超过 64")
    private String realName;

    @Schema(description = "角色 DOCTOR/ADMIN")
    @Pattern(regexp = "DOCTOR|ADMIN", message = "角色仅支持 DOCTOR/ADMIN")
    private String role;

    @Schema(description = "手机号（提供则更新）")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "状态 ENABLED/DISABLED")
    @Pattern(regexp = "ENABLED|DISABLED", message = "状态仅支持 ENABLED/DISABLED")
    private String status;

    @Schema(description = "新密码（提供则重置，明文）")
    @Size(min = 6, max = 64, message = "密码长度需为 6-64")
    private String password;
}
