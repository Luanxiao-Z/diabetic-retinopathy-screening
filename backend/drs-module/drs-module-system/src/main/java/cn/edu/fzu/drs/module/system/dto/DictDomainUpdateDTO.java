package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改字典域入参（未提供字段不覆盖）。
 */
@Data
@Schema(description = "修改字典域请求")
public class DictDomainUpdateDTO {

    @Schema(description = "字典域名称")
    @Size(max = 64, message = "名称长度不能超过 64")
    private String domainName;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注过长")
    private String remark;
}
