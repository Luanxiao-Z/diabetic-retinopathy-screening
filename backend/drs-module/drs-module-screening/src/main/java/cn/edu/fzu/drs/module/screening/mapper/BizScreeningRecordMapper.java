package cn.edu.fzu.drs.module.screening.mapper;

import cn.edu.fzu.drs.module.screening.entity.BizScreeningRecordEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 筛查记录 Mapper。阶段 1 仅落地数据层，业务方法在阶段 4 补充。
 */
@Mapper
public interface BizScreeningRecordMapper extends BaseMapper<BizScreeningRecordEntity> {
}
