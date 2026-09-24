package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.system.dto.OperationLogPageQuery;
import cn.edu.fzu.drs.module.system.entity.SysOperationLogEntity;
import cn.edu.fzu.drs.module.system.mapper.SysOperationLogMapper;
import cn.edu.fzu.drs.module.system.service.OperationLogService;
import cn.edu.fzu.drs.module.system.vo.OperationLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作审计日志实现。
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 模块中文名 */
    private static final Map<String, String> MODULE_NAMES = Map.of(
            "AUTH", "认证",
            "SCREENING", "筛查业务",
            "USER", "用户管理",
            "DICT", "字典管理"
    );

    /** 动作中文名 */
    private static final Map<String, String> ACTION_NAMES = Map.ofEntries(
            Map.entry("LOGIN", "登录"),
            Map.entry("LOGOUT", "退出登录"),
            Map.entry("UPLOAD", "上传筛查"),
            Map.entry("DELETE", "删除"),
            Map.entry("EXPORT", "导出"),
            Map.entry("CREATE", "新增"),
            Map.entry("UPDATE", "修改"),
            Map.entry("CHANGE_STATE", "启停变更"),
            Map.entry("REVIEW", "人工复核")
    );

    private final SysOperationLogMapper mapper;

    public OperationLogServiceImpl(SysOperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(String module, String action, String target, boolean success,
                       String errorMsg, long costMs) {
        AuthPrincipal principal = AuthContext.get();
        record(module, action, target, success, errorMsg, costMs,
                principal == null ? null : principal.getUsername());
    }

    @Override
    public void record(String module, String action, String target, boolean success,
                       String errorMsg, long costMs, String username) {
        try {
            SysOperationLogEntity entity = new SysOperationLogEntity();
            entity.setId(IdGenerator.nextId());
            entity.setUsername(username);
            entity.setModule(module);
            entity.setAction(action);
            entity.setTarget(target);
            entity.setResult(success ? "SUCCESS" : "FAIL");
            entity.setErrorMsg(truncate(errorMsg, 500));
            entity.setIp(currentIp());
            entity.setCostMs(costMs);
            entity.setCreateTime(LocalDateTime.now());
            entity.setDeleteFlag("N");
            mapper.insert(entity);
        } catch (Exception e) {
            // 审计写入失败绝不阻断主流程
            log.warn("操作日志写入失败（已忽略）：module={} action={} err={}", module, action, e.getMessage());
        }
    }

    @Override
    public PageResult<OperationLogVO> page(OperationLogPageQuery query) {
        LambdaQueryWrapper<SysOperationLogEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getUsername() != null && !query.getUsername().isBlank()) {
            wrapper.like(SysOperationLogEntity::getUsername, query.getUsername());
        }
        if (query.getModule() != null && !query.getModule().isBlank()) {
            wrapper.eq(SysOperationLogEntity::getModule, query.getModule());
        }
        if (query.getResult() != null && !query.getResult().isBlank()) {
            wrapper.eq(SysOperationLogEntity::getResult, query.getResult());
        }
        if (query.getStartDate() != null && !query.getStartDate().isBlank()) {
            wrapper.ge(SysOperationLogEntity::getCreateTime, parse(query.getStartDate()));
        }
        if (query.getEndDate() != null && !query.getEndDate().isBlank()) {
            wrapper.le(SysOperationLogEntity::getCreateTime, parse(query.getEndDate()));
        }
        wrapper.orderByDesc(SysOperationLogEntity::getCreateTime);

        long current = query.getCurrent() == null ? 1L : query.getCurrent();
        long size = query.getPageSize() == null ? 20L : query.getPageSize();
        Page<SysOperationLogEntity> page = new Page<>(current, size);
        mapper.selectPage(page, wrapper);

        List<OperationLogVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        PageResult<OperationLogVO> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setList(list);
        return result;
    }

    private OperationLogVO toVO(SysOperationLogEntity e) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(e.getId());
        vo.setUsername(e.getUsername());
        vo.setModule(e.getModule());
        vo.setModuleName(MODULE_NAMES.getOrDefault(e.getModule(), e.getModule()));
        vo.setAction(e.getAction());
        vo.setActionName(ACTION_NAMES.getOrDefault(e.getAction(), e.getAction()));
        vo.setTarget(e.getTarget());
        vo.setResult(e.getResult());
        vo.setErrorMsg(e.getErrorMsg());
        vo.setIp(e.getIp());
        vo.setCostMs(e.getCostMs());
        vo.setCreateTime(e.getCreateTime());
        return vo;
    }

    /** 取客户端 IP，兼容反向代理场景 */
    private String currentIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private LocalDateTime parse(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FMT);
        } catch (Exception e) {
            throw new BusinessException(400, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
    }
}
