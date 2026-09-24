package cn.edu.fzu.drs.module.system.entity;

import cn.edu.fzu.drs.module.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 操作审计日志实体（对应表 sys_operation_log）。
 * <p>记录关键业务动作（登录、筛查上传/删除/导出、用户与字典变更），
 * 用于问题追溯与合规审计。</p>
 */
@Getter
@Setter
@TableName("sys_operation_log")
public class SysOperationLogEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 操作人用户名（未登录场景如登录失败时可为空） */
    private String username;

    /** 模块：AUTH / SCREENING / USER / DICT */
    private String module;

    /** 动作编码：LOGIN / LOGOUT / UPLOAD / DELETE / EXPORT / CREATE / UPDATE / CHANGE_STATE */
    private String action;

    /** 操作对象描述（如患者姓名、账号名、字典编码） */
    private String target;

    /** 结果：SUCCESS / FAIL */
    private String result;

    /** 失败原因（成功时为空） */
    private String errorMsg;

    /** 客户端 IP */
    private String ip;

    /** 耗时（毫秒） */
    private Long costMs;
}
