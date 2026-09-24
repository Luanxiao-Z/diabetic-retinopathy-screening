package cn.edu.fzu.drs.module.screening.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 患者随访聚合视图：按患者归并其历次筛查，用于纵向随访管理。
 * <p>DR 为进展性疾病，单次筛查只能反映当前状态；按患者纵向对比分级变化
 * 才能识别「进展」与「好转」，这是随访的核心临床价值。</p>
 */
@Data
@Schema(description = "患者随访聚合信息")
public class PatientFollowUpVO {

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "患者性别（取最近一次记录）")
    private String patientGender;

    @Schema(description = "患者年龄（取最近一次记录）")
    private Integer patientAge;

    @Schema(description = "累计筛查次数")
    private Long totalCount;

    @Schema(description = "最近一次筛查时间")
    private LocalDateTime latestTime;

    @Schema(description = "首次筛查时间")
    private LocalDateTime firstTime;

    @Schema(description = "最近一次分级编码")
    private String latestLevel;

    @Schema(description = "最近一次分级中文说明")
    private String latestLevelName;

    @Schema(description = "最近一次置信度")
    private BigDecimal latestConfidence;

    @Schema(description = "最近一次转诊建议编码")
    private String latestSuggestion;

    @Schema(description = "最近一次转诊建议中文说明")
    private String latestSuggestionName;

    @Schema(description = "上一次分级编码（仅一次筛查时为 null）")
    private String previousLevel;

    @Schema(description = "上一次分级中文说明")
    private String previousLevelName;

    @Schema(description = "较上一次是否发生变化")
    private Boolean levelChanged;

    @Schema(description = "分级变化方向：UP 进展 / DOWN 好转 / SAME 持平 / FIRST 首次")
    private String trendDirection;

    @Schema(description = "该患者需人工复核的记录数")
    private Long needReviewCount;
}
