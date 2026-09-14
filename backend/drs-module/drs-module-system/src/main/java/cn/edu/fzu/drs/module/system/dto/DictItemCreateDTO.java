package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增字典项入参。
 */
@Data
@Schema(description = "新增字典项请求")
public class DictItemCreateDTO {

    @Schema(description = "字典项编码（字符型，区分大小写）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典项编码不能为空")
    @Pattern(regexp = "[A-Za-z0-9_]+", message = "编码仅限字母、数字、下划线")
    @Size(max = 64, message = "编码长度不能超过 64")
    private String itemCode;

    @Schema(description = "字典项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "字典项名称不能为空")
    @Size(max = 64, message = "名称长度不能超过 64")
    private String itemName;

    @Schema(description = "扩展值（如分级对应的数值 0-4）")
    @Size(max = 64, message = "扩展值过长")
    private String itemValue;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "备注")
    @Size(max = 255, message = "备注过长")
    private String remark;
}
