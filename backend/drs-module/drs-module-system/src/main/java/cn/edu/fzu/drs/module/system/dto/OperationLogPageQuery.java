package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 操作日志分页查询条件（GET 参数绑定）。
 */
@Data
@Schema(description = "操作日志查询条件")
public class OperationLogPageQuery {

    @Schema(description = "操作人用户名（模糊匹配）")
    private String username;

    @Schema(description = "模块编码（AUTH/SCREENING/USER/DICT）")
    private String module;

    @Schema(description = "结果（SUCCESS/FAIL）")
    private String result;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startDate;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String endDate;

    @Schema(description = "当前页（从 1 开始）", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "20")
    private Long pageSize = 20L;
}
