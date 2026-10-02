package cn.edu.fzu.drs.module.screening.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 随访待办条目（GET /api/v1/biz/screening-records/todos）。
 *
 * <p>三类待办的数据来源不同（待复核与需转诊来自筛查记录，逾期未复诊来自患者聚合），
 * 在此统一为同一结构，便于前端以分页表格呈现。</p>
 */
@Data
@NoArgsConstructor
public class TodoItemVO {

    /** 唯一键（跨类型不重复，用于前端列表 key） */
    private String id;

    /** 类型：review（待人工复核）/ referral（需转诊）/ overdue（逾期未复诊） */
    private String type;

    /** 类型中文名 */
    private String typeName;

    private String patientName;

    /** 分级中文名 */
    private String levelName;

    /** 补充说明（不确定性、转诊建议、逾期天数等） */
    private String detail;

    /** 不确定性（归一化预测熵），仅待复核条目有值 */
    private BigDecimal uncertainty;

    /** 排序与展示用时间：筛查记录创建时间 / 患者最近筛查时间 */
    private LocalDateTime time;

    /** 关联的筛查记录 id（待复核、需转诊可跳转诊断报告） */
    private String recordId;
}
