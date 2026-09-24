package cn.edu.fzu.drs.module.system.mapper;

import cn.edu.fzu.drs.module.system.entity.SysOperationLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper。
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLogEntity> {
}
