package cn.edu.fzu.drs.module.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改字典项入参（未提供字段不覆盖）。
 */
@Data
@Schema(description = "修改字典项请求")
public class DictItemUpdateDTO {

    @Schema(description = "字典项名称")
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
