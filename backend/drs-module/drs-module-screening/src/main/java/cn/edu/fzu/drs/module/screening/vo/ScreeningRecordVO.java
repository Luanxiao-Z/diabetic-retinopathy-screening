package cn.edu.fzu.drs.module.screening.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 筛查记录出参（含私有桶预签名访问 URL 与中文分级/建议说明）。
 */
@Data
@Schema(description = "筛查记录详情")
public class ScreeningRecordVO {

    @Schema(description = "记录主键")
    private String id;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "患者年龄")
    private Integer patientAge;

    @Schema(description = "患者性别（字典 C_GENDER：MALE/FEMALE）")
    private String patientGender;

    @Schema(description = "原始图片对象键")
    private String imageKey;

    @Schema(description = "原始图片预签名访问 URL（有时效）")
    private String imageUrl;

    @Schema(description = "热力图对象键")
    private String gradCamKey;

    @Schema(description = "热力图预签名访问 URL（有时效，可能为 null）")
    private String gradCamUrl;

    @Schema(description = "DR 分级编码（LEVEL_0..LEVEL_4）")
    private String resultLevel;

    @Schema(description = "DR 分级中文说明")
    private String levelName;

    @Schema(description = "最高概率置信度")
    private BigDecimal confidence;

    @Schema(description = "是否需人工复核（置信度低于阈值时为 true）")
    private Boolean needReview;

    @Schema(description = "人工复核阈值（低于该值需复核）")
    private BigDecimal reviewThreshold;

    @Schema(description = "各类别概率（LEVEL_0..LEVEL_4）")
    private Map<String, Double> probabilities;

    @Schema(description = "转诊建议编码（REVIEW/CLINIC/REFERRAL）")
    private String suggestion;

    @Schema(description = "转诊建议中文说明")
    private String suggestionName;

    @Schema(description = "模型版本")
    private String modelVersion;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "人工复核状态：PENDING/CONFIRMED；为空表示无需复核")
    private String reviewStatus;

    @Schema(description = "人工复核状态中文说明")
    private String reviewStatusName;

    @Schema(description = "复核人")
    private String reviewer;

    @Schema(description = "复核时间")
    private LocalDateTime reviewTime;

    @Schema(description = "复核意见")
    private String reviewRemark;
}
