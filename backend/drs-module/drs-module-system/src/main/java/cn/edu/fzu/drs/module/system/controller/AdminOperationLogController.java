package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.constant.PermissionConstants;
import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.security.annotation.RequirePermission;
import cn.edu.fzu.drs.module.system.dto.OperationLogPageQuery;
import cn.edu.fzu.drs.module.system.service.OperationLogService;
import cn.edu.fzu.drs.module.system.vo.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口（需 admin:log:view 权限）。
 */
@Tag(name = "操作日志", description = "关键业务动作的审计查询（需 admin:log:view 权限）")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminOperationLogController {

    private final OperationLogService operationLogService;

    public AdminOperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Operation(summary = "分页查询操作日志", description = "支持按操作人、模块、结果与时间范围过滤，按时间倒序")
    @GetMapping("/operation-logs")
    @RequirePermission(PermissionConstants.ADMIN_LOG_VIEW)
    public Result<PageResult<OperationLogVO>> page(OperationLogPageQuery query) {
        return Result.ok(operationLogService.page(query));
    }
}
