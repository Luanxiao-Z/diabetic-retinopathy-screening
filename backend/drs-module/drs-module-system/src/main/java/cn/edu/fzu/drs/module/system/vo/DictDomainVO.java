package cn.edu.fzu.drs.module.system.vo;

/**
 * 字典域出参。
 */
public class DictDomainVO {

    private String domainCode;
    private String domainName;
    private String remark;

    public DictDomainVO() {
    }

    public DictDomainVO(String domainCode, String domainName, String remark) {
        this.domainCode = domainCode;
        this.domainName = domainName;
        this.remark = remark;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
