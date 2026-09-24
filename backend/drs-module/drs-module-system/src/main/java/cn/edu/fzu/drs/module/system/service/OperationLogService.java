package cn.edu.fzu.drs.module.system.service;

import cn.edu.fzu.drs.module.common.audit.OperationLogRecorder;
import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.system.dto.OperationLogPageQuery;
import cn.edu.fzu.drs.module.system.vo.OperationLogVO;

/**
 * 操作审计日志服务：在 common 模块的跨模块契约之上，追加管理端查询能力。
 * <p>写入失败**不得影响主业务流程**，实现内部吞掉异常并降级为告警日志。</p>
 */
public interface OperationLogService extends OperationLogRecorder {

    /** 分页查询日志（时间倒序）。 */
    PageResult<OperationLogVO> page(OperationLogPageQuery query);
}
