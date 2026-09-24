package cn.edu.fzu.drs.module.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志出参。
 */
@Data
@Schema(description = "操作审计日志")
public class OperationLogVO {

    @Schema(description = "日志主键")
    private String id;

    @Schema(description = "操作人用户名")
    private String username;

    @Schema(description = "模块编码（AUTH/SCREENING/USER/DICT）")
    private String module;

    @Schema(description = "模块中文名")
    private String moduleName;

    @Schema(description = "动作编码")
    private String action;

    @Schema(description = "动作中文名")
    private String actionName;

    @Schema(description = "操作对象描述")
    private String target;

    @Schema(description = "结果（SUCCESS/FAIL）")
    private String result;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "客户端 IP")
    private String ip;

    @Schema(description = "耗时（毫秒）")
    private Long costMs;

    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
