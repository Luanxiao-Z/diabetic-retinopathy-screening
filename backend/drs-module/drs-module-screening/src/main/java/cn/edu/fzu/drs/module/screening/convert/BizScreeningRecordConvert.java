package cn.edu.fzu.drs.module.screening.convert;

import cn.edu.fzu.drs.module.screening.entity.BizScreeningRecordEntity;
import cn.edu.fzu.drs.module.screening.vo.ScreeningRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 筛查记录 Entity ↔ VO 转换（手写）。
 * <p>含 DR 分级 / 转诊建议中文映射，以及概率 JSON 字符串到 Map 的反序列化。</p>
 */
public final class BizScreeningRecordConvert {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** DR 分级中文说明（与字典 B_DR_LEVEL 保持一致） */
    public static final Map<String, String> LEVEL_NAMES = Map.of(
            "LEVEL_0", "正常(Normal)",
            "LEVEL_1", "轻度 NPDR",
            "LEVEL_2", "中度 NPDR",
            "LEVEL_3", "重度 NPDR",
            "LEVEL_4", "PDR(增殖性)"
    );

    /** 转诊建议中文说明（与字典 B_DR_SUGGESTION 保持一致） */
    public static final Map<String, String> SUGGESTION_NAMES = Map.of(
            "REVIEW", "定期复查",
            "CLINIC", "建议眼科就诊",
            "REFERRAL", "建议尽快转诊上级医院"
    );

    private BizScreeningRecordConvert() {
    }

    public static ScreeningRecordVO toVO(BizScreeningRecordEntity entity, String imageUrl, String gradCamUrl) {
        if (entity == null) {
            return null;
        }
        ScreeningRecordVO vo = new ScreeningRecordVO();
        vo.setId(entity.getId());
        vo.setPatientName(entity.getPatientName());
        vo.setPatientAge(entity.getPatientAge());
        vo.setPatientGender(entity.getPatientGender());
        vo.setImageKey(entity.getImageKey());
        vo.setImageUrl(imageUrl);
        vo.setGradCamKey(entity.getGradCamKey());
        vo.setGradCamUrl(gradCamUrl);
        vo.setResultLevel(entity.getResultLevel());
        vo.setLevelName(LEVEL_NAMES.get(entity.getResultLevel()));
        vo.setConfidence(entity.getConfidence());
        vo.setProbabilities(parseProbabilities(entity.getProbabilities()));
        vo.setSuggestion(entity.getSuggestion());
        vo.setSuggestionName(SUGGESTION_NAMES.get(entity.getSuggestion()));
        vo.setModelVersion(entity.getModelVersion());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private static Map<String, Double> parseProbabilities(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Double>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * 将概率 Map 序列化为 JSON 字符串（落库 probabilities 字段）。
     */
    public static String toProbabilitiesJson(Map<String, Double> probabilities) {
        if (probabilities == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(probabilities);
        } catch (Exception e) {
            return null;
        }
    }
}
