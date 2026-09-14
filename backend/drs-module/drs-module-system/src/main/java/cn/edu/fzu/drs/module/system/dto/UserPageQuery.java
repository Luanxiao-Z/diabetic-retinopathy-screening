package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分页查询条件（GET 查询参数绑定）。
 */
@Data
@Schema(description = "用户分页查询条件")
public class UserPageQuery {

    @Schema(description = "用户名模糊匹配")
    private String username;

    @Schema(description = "角色精确匹配 DOCTOR/ADMIN")
    private String role;

    @Schema(description = "状态精确匹配 ENABLED/DISABLED")
    private String status;

    @Schema(description = "当前页，从 1 开始，默认 1")
    private Integer current = 1;

    @Schema(description = "每页条数，默认 10，最大 100")
    private Integer pageSize = 10;
}
