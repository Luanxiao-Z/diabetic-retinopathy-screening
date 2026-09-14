package cn.edu.fzu.drs.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典域出参。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictDomainVO {

    private String domainCode;
    private String domainName;
    private String remark;
}
