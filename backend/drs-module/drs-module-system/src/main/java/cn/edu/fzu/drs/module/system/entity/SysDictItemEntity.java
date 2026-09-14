package cn.edu.fzu.drs.module.system.entity;

import cn.edu.fzu.drs.module.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典项实体（对应表 sys_dict_item）。
 */
@Getter
@Setter
@TableName("sys_dict_item")
public class SysDictItemEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 字典域编码（区分大小写） */
    private String domainCode;

    /** 字典项编码（区分大小写） */
    private String itemCode;

    /** 字典项名称 */
    private String itemName;

    /** 扩展值（如分级对应的数值 0-4） */
    private String itemValue;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;
}
