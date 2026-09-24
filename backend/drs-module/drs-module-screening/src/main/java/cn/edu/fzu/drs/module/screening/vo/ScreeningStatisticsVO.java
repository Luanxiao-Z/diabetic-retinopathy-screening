package cn.edu.fzu.drs.module.screening.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 筛查统计出参。
 */
@Data
@Schema(description = "筛查统计结果")
public class ScreeningStatisticsVO {

    @Schema(description = "记录总数（当前数据权限与筛选范围内）")
    private Long total;

    @Schema(description = "各级别人数分布（LEVEL_0..LEVEL_4 → 数量）")
    private Map<String, Long> levelDistribution;

    @Schema(description = "转诊建议分布（REVIEW/CLINIC/REFERRAL → 数量）")
    private Map<String, Long> suggestionDistribution;

    @Schema(description = "转诊率 = REFERRAL 数量 / 总数（4 位小数）")
    private BigDecimal referralRate;

    @Schema(description = "近 30 天每日筛查趋势（升序）")
    private List<DailyCountVO> trend;

    @Schema(description = "需人工复核数量（置信度低于阈值）")
    private Long needReviewCount;

    @Schema(description = "人工复核阈值")
    private BigDecimal reviewThreshold;
}
