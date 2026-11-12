package cn.edu.fzu.drs.module.screening.entity;

import cn.edu.fzu.drs.module.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 筛查记录实体（对应表 biz_screening_record）。阶段 1 仅落地数据层，业务接口在阶段 4 实现。
 */
@Setter
@Getter
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

    /** 最高概率置信度（top1 softmax） */
    private BigDecimal confidence;

    /** 归一化预测熵（0~1），越大越不确定；人工复核判定依据 */
    private BigDecimal uncertainty;

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

    /** 人工复核状态：PENDING（待复核）/ CONFIRMED（已复核）；null 表示无需复核 */
    private String reviewStatus;

    /** 复核人 username */
    private String reviewer;

    /** 复核时间 */
    private LocalDateTime reviewTime;

    /** 复核意见 */
    private String reviewRemark;

}
