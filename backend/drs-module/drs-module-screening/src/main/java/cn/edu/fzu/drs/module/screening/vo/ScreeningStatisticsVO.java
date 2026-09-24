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

    /** 环比：近 30 天数量 */
    @Schema(description = "近 30 天筛查数量")
    private Long recentTotal;

    /** 环比：前 30 天数量 */
    @Schema(description = "前 30 天筛查数量（用于环比）")
    private Long prevTotal;

    /** 环比增长率 =（近30天 - 前30天）/ 前30天；前30天为 0 时记为 1（新增）或 0（持平） */
    @Schema(description = "环比增长率（4 位小数，可为负）")
    private BigDecimal growthRate;

    @Schema(description = "人工复核阈值")
    private BigDecimal reviewThreshold;
}
