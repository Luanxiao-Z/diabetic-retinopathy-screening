package cn.edu.fzu.drs.module.screening.mapper;

import cn.edu.fzu.drs.module.screening.entity.BizScreeningRecordEntity;
import cn.edu.fzu.drs.module.screening.vo.DailyCountVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 筛查记录 Mapper。
 *
 * <p>统计类聚合全部下推为 SQL GROUP BY（通过 ${ew.customSqlSegment} 复用 Service 层
 * 的筛选与数据权限条件），避免把全量记录拉回内存聚合。</p>
 */
@Mapper
public interface BizScreeningRecordMapper extends BaseMapper<BizScreeningRecordEntity> {

    /**
     * 近 30 天逐日计数。外层限定起始时间，内层子查询复用筛选与数据权限条件；
     * 缺失日期由调用方补 0。
     */
    @Select("""
            SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS count
            FROM biz_screening_record
            WHERE create_time >= #{from}
              AND id IN (SELECT id FROM biz_screening_record ${ew.customSqlSegment})
            GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')
            """)
    List<DailyCountVO> selectDailyCounts(@Param("from") LocalDateTime from,
                                         @Param(Constants.WRAPPER) Wrapper<BizScreeningRecordEntity> wrapper);

    /** 分级分布 */
    @Select("""
            SELECT result_level AS code, COUNT(*) AS count
            FROM biz_screening_record
            WHERE result_level IS NOT NULL
              AND id IN (SELECT id FROM biz_screening_record ${ew.customSqlSegment})
            GROUP BY result_level
            """)
    List<Map<String, Object>> selectLevelDistribution(
            @Param(Constants.WRAPPER) Wrapper<BizScreeningRecordEntity> wrapper);

    /** 转诊建议分布 */
    @Select("""
            SELECT suggestion AS code, COUNT(*) AS count
            FROM biz_screening_record
            WHERE suggestion IS NOT NULL
              AND id IN (SELECT id FROM biz_screening_record ${ew.customSqlSegment})
            GROUP BY suggestion
            """)
    List<Map<String, Object>> selectSuggestionDistribution(
            @Param(Constants.WRAPPER) Wrapper<BizScreeningRecordEntity> wrapper);

    /** 待人工复核数量：不确定性达阈值且未确认复核（与列表页筛选同一口径） */
    @Select("""
            SELECT COUNT(*)
            FROM biz_screening_record
            WHERE uncertainty >= #{threshold}
              AND (review_status IS NULL OR review_status = 'PENDING')
              AND id IN (SELECT id FROM biz_screening_record ${ew.customSqlSegment})
            """)
    Long selectNeedReviewCount(@Param("threshold") BigDecimal threshold,
                               @Param(Constants.WRAPPER) Wrapper<BizScreeningRecordEntity> wrapper);
}
