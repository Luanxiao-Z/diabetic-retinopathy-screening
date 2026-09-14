package cn.edu.fzu.drs.module.common.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 实体基类：统一主键与审计字段。
 * <p>主键为 CHAR(32) UUID（去掉连字符），编码类字段统一使用 {@code *_as_cs} 排序规则；
 * 逻辑删除标记 {@code deleteFlag} 取值 {@code N}/{@code Y}，不建物理外键（见数据库设计规范）。</p>
 */
@Getter
@Setter
public abstract class BaseEntity {

    private String id;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String deleteFlag;
}
