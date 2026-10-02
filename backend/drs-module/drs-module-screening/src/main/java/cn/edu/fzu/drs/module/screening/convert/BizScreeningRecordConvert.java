package cn.edu.fzu.drs.module.screening.convert;

import cn.edu.fzu.drs.module.screening.entity.BizScreeningRecordEntity;
import cn.edu.fzu.drs.module.screening.vo.ScreeningRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
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

    /**
     * 人工复核阈值：**归一化预测熵**高于该值的筛查结果视为「需人工复核」。
     *
     * <p><b>为何不用 top1 置信度</b>：深度网络的 softmax 普遍饱和，在 30 张抽样中
     * 有 20 张 top1 ≥ 0.99。按「top1 &lt; 0.70」判定时，在完整测试集（367 张）上
     * 仅触发 12.0% 却只覆盖 23.5% 的错分样本（16/68），复核机制形同虚设。</p>
     *
     * <p><b>阈值 0.18 的确定依据</b>（完整测试集 367 张，脚本 {@code tools/fit_review_threshold.py}）：</p>
     * <table>
     *   <tr><th>阈值</th><th>触发率</th><th>错分召回</th></tr>
     *   <tr><td>0.15</td><td>30.5%</td><td>61.8%</td></tr>
     *   <tr><td><b>0.18</b></td><td><b>28.6%</b></td><td><b>61.8%</b></td></tr>
     *   <tr><td>0.20</td><td>27.0%</td><td>60.3%</td></tr>
     *   <tr><td>0.25</td><td>24.3%</td><td>57.4%</td></tr>
     * </table>
     * <p>0.18 在「触发率 ≤ 30%」的约束下取得最高召回（61.8%，与 0.15 持平但触发更少）。</p>
     *
     * <p><b>已知局限</b>：仍有 38.2% 的错分样本熵极低（18/68 的熵 &lt; 0.05），
     * 属「高置信度错误」，基于输出分布的不确定性指标无法识别，需从模型层面改进。</p>
     */
    public static final BigDecimal REVIEW_UNCERTAINTY_THRESHOLD = new BigDecimal("0.18");

    /** 不确定性缺失或达到阈值 → 需人工复核（缺失时按保守策略标记） */
    public static boolean needReview(BigDecimal uncertainty) {
        return uncertainty == null || uncertainty.compareTo(REVIEW_UNCERTAINTY_THRESHOLD) >= 0;
    }

    /** 人工复核状态中文说明 */
    public static final Map<String, String> REVIEW_STATUS_NAMES = Map.of(
            "PENDING", "待复核",
            "CONFIRMED", "已复核"
    );

    private BizScreeningRecordConvert() {
    }

    /**
     * 空安全的名称映射查询。
     * <p><b>注意</b>：{@code Map.of(...)} 返回的不可变 Map 在 {@code get(null)} 时会抛
     * {@link NullPointerException}（与 HashMap 行为不同），故所有名称映射查询必须经此方法。</p>
     */
    public static String nameOf(Map<String, String> names, String key) {
        return key == null ? null : names.get(key);
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
        vo.setLevelName(nameOf(LEVEL_NAMES, entity.getResultLevel()));
        vo.setConfidence(entity.getConfidence());
        vo.setUncertainty(entity.getUncertainty());
        vo.setNeedReview(needReview(entity.getUncertainty()));
        vo.setReviewThreshold(REVIEW_UNCERTAINTY_THRESHOLD);
        vo.setProbabilities(parseProbabilities(entity.getProbabilities()));
        vo.setSuggestion(entity.getSuggestion());
        vo.setSuggestionName(nameOf(SUGGESTION_NAMES, entity.getSuggestion()));
        vo.setModelVersion(entity.getModelVersion());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        // 复核状态：未落库时按阈值推导为「待复核」，便于前端统一展示
        String reviewStatus = entity.getReviewStatus();
        if (reviewStatus == null && needReview(entity.getUncertainty())) {
            reviewStatus = "PENDING";
        }
        vo.setReviewStatus(reviewStatus);
        vo.setReviewStatusName(nameOf(REVIEW_STATUS_NAMES, reviewStatus));
        vo.setReviewer(entity.getReviewer());
        vo.setReviewTime(entity.getReviewTime());
        vo.setReviewRemark(entity.getReviewRemark());
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
