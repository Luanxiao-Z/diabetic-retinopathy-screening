package cn.edu.fzu.drs.module.system.entity;

import cn.edu.fzu.drs.module.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/**
 * 字典域实体（对应表 sys_dict_domain）。
 */
@Getter
@Setter
@TableName("sys_dict_domain")
public class SysDictDomainEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 字典域编码（区分大小写） */
    private String domainCode;

    /** 字典域名称 */
    private String domainName;

    /** 备注 */
    private String remark;
}
