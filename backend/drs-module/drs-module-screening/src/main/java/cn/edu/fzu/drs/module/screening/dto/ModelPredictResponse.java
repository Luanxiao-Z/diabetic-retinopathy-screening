package cn.edu.fzu.drs.module.screening.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 模型推理服务 {@code POST /predict} 响应契约（与 model-service/app/schemas.py 对齐）。
 * <p>JSON 字段为 snake_case，使用 {@link JsonProperty} 映射。</p>
 */
@Data
public class ModelPredictResponse {

    @JsonProperty("record_id")
    private String recordId;

    @JsonProperty("result_level")
    private String resultLevel;

    @JsonProperty("result_label")
    private String resultLabel;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("probabilities")
    private Map<String, Double> probabilities;

    @JsonProperty("suggestion")
    private String suggestion;

    @JsonProperty("model_version")
    private String modelVersion;
}
