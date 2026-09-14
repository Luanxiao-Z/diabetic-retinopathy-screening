package cn.edu.fzu.drs.module.screening.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 筛查记录分页/统计查询条件（GET 参数绑定）。
 * <p>数据权限 SELF/ALL 由 Service 层依据当前登录主体追加 {@code user_id} 过滤，不在此透传。</p>
 */
@Data
@Schema(description = "筛查记录查询条件")
public class ScreeningPageQuery {

    @Schema(description = "患者姓名（模糊匹配）")
    private String patientName;

    @Schema(description = "DR 分级（字典 B_DR_LEVEL：LEVEL_0..LEVEL_4）")
    private String level;

    @Schema(description = "开始时间 yyyy-MM-dd HH:mm:ss")
    private String startDate;

    @Schema(description = "结束时间 yyyy-MM-dd HH:mm:ss")
    private String endDate;

    @Schema(description = "当前页（从 1 开始）", example = "1")
    private Long current = 1L;

    @Schema(description = "每页条数", example = "10")
    private Long pageSize = 10L;
}
