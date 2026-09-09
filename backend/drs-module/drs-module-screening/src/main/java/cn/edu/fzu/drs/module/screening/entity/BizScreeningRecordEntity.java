package cn.edu.fzu.drs.module.screening.entity;

import cn.edu.fzu.drs.module.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 筛查记录实体（对应表 biz_screening_record）。阶段 1 仅落地数据层，业务接口在阶段 4 实现。
 */
@TableName("biz_screening_record")
public class BizScreeningRecordEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 操作医生 username（数据权限 SELF 依据） */
    private String userId;

    /** 患者姓名 */
    private String patientName;

    /** 患者年龄 */
    private Integer patientAge;

    /** 患者性别（字典:C_GENDER）MALE/FEMALE */
    private String patientGender;

    /** 原始图片 MinIO object key */
    private String imageKey;

    /** 图片访问 URL */
    private String imageUrl;

    /** DR 分级（字典:B_DR_LEVEL）LEVEL_0..LEVEL_4 */
    private String resultLevel;

    /** 最高概率置信度 */
    private BigDecimal confidence;

    /** 各类别概率 JSON */
    private String probabilities;

    /** 转诊建议（字典:B_DR_SUGGESTION） */
    private String suggestion;

    /** 热力图 MinIO object key */
    private String gradCamKey;

    /** 模型版本 */
    private String modelVersion;

    /** 备注 */
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Integer getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public void setResultLevel(String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(String probabilities) {
        this.probabilities = probabilities;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getGradCamKey() {
        return gradCamKey;
    }

    public void setGradCamKey(String gradCamKey) {
        this.gradCamKey = gradCamKey;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
