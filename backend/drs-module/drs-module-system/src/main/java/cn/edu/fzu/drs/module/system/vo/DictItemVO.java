package cn.edu.fzu.drs.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典项出参。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO {

    private String domainCode;
    private String itemCode;
    private String itemName;
    private String itemValue;
    private Integer sort;
}
