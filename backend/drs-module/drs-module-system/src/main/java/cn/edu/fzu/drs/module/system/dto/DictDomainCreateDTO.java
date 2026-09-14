package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增字典域入参。
 */
@Data
@Schema(description = "新增字典域请求")
public class DictDomainCreateDTO {

    @Schema(description = "字典域编码（字符型，区分大小写）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典域编码不能为空")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "编码仅限字母、数字、下划线")
    @Size(max = 64, message = "编码长度不能超过 64")
    private String domainCode;

    @Schema(description = "字典域名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典域名称不能为空")
    @Size(max = 64, message = "名称长度不能超过 64")
    private String domainName;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注过长")
    private String remark;
}
