package cn.edu.fzu.drs.module.common.entity;

import java.time.LocalDateTime;

/**
 * 实体基类：统一主键与审计字段。
 * <p>主键为 CHAR(32) UUID（去掉连字符），编码类字段统一使用 {@code *_as_cs} 排序规则；
 * 逻辑删除标记 {@code deleteFlag} 取值 {@code N}/{@code Y}，不建物理外键（见数据库设计规范）。</p>
 */
public abstract class BaseEntity {

    private String id;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private String deleteFlag;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(String deleteFlag) {
        this.deleteFlag = deleteFlag;
    }
}
