package cn.edu.fzu.drs.module.screening.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日筛查计数（趋势图用）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "每日筛查计数")
public class DailyCountVO {

    @Schema(description = "日期 yyyy-MM-dd")
    private String date;

    @Schema(description = "当日筛查记录数")
    private Long count;
}
