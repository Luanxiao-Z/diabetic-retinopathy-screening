package cn.edu.fzu.drs.module.system.vo;

/**
 * 字典项出参。
 */
public class DictItemVO {

    private String domainCode;
    private String itemCode;
    private String itemName;
    private String itemValue;
    private Integer sort;

    public DictItemVO() {
    }

    public DictItemVO(String domainCode, String itemCode, String itemName, String itemValue, Integer sort) {
        this.domainCode = domainCode;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.itemValue = itemValue;
        this.sort = sort;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
