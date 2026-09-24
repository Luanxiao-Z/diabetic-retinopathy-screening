package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.audit.OperationLogRecorder;
import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.common.util.PasswordUtil;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.security.service.TokenService;
import cn.edu.fzu.drs.module.system.dto.LoginDTO;
import cn.edu.fzu.drs.module.system.dto.RegisterDTO;
import cn.edu.fzu.drs.module.system.entity.SysUserEntity;
import cn.edu.fzu.drs.module.system.mapper.SysUserMapper;
import cn.edu.fzu.drs.module.system.service.AuthService;
import cn.edu.fzu.drs.module.system.vo.LoginVO;
import cn.edu.fzu.drs.module.system.vo.RegisterVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * 认证服务实现：校验凭证后通过 {@link TokenService} 签发 UUID 令牌（Session 模式，存 Redis）。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String STATUS_ENABLED = "ENABLED";

    /** 自助注册固定为普通医生角色，避免通过注册接口越权获得管理员权限 */
    private static final String ROLE_DOCTOR = "DOCTOR";

    private final SysUserMapper userMapper;
    private final TokenService tokenService;
    private final OperationLogRecorder audit;

    public AuthServiceImpl(SysUserMapper userMapper, TokenService tokenService,
                           OperationLogRecorder audit) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.audit = audit;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        long startedAt = System.currentTimeMillis();
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, dto.getUsername()));
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            // 登录失败同样留痕（安全审计关注点）
            audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_LOGIN,
                    dto.getUsername(), false, "用户名或密码错误",
                    System.currentTimeMillis() - startedAt, dto.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_LOGIN,
                    dto.getUsername(), false, "账号已被停用",
                    System.currentTimeMillis() - startedAt, dto.getUsername());
            throw new BusinessException(401, "账号已被停用");
        }
        String token = tokenService.createToken(user.getId(), user.getUsername(), user.getRole());
        AuthPrincipal principal = tokenService.getPrincipal(token);
        audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_LOGIN,
                user.getUsername(), true, null,
                System.currentTimeMillis() - startedAt, user.getUsername());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole(),
                new ArrayList<>(principal.getPermissions()), principal.getDataScope());
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            tokenService.remove(token);
        }
        audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_LOGOUT,
                null, true, null, 0L);
    }

    @Override
    public RegisterVO register(RegisterDTO dto) {
        long startedAt = System.currentTimeMillis();
        String username = dto.getUsername().trim();

        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, username));
        if (count != null && count > 0) {
            audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_REGISTER,
                    username, false, "用户名已存在",
                    System.currentTimeMillis() - startedAt, username);
            throw new BusinessException(409, "用户名已存在，请更换后重试");
        }

        SysUserEntity entity = new SysUserEntity();
        entity.setUsername(username);
        entity.setRealName(dto.getRealName().trim());
        entity.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        entity.setRole(ROLE_DOCTOR);
        entity.setStatus(STATUS_ENABLED);
        entity.setPassword(PasswordUtil.encode(dto.getPassword()));
        // 本工程未启用 MyBatis-Plus 的 MetaObjectHandler，主键与审计字段须显式赋值，
        // 否则插入时 id 为 null 会触发 Column 'id' cannot be null。
        entity.setId(IdGenerator.nextId());
        entity.setDeleteFlag("N");
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发注册同一用户名时由唯一索引兜底（前置 selectCount 存在竞态窗口）
            audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_REGISTER,
                    username, false, "用户名已存在（并发冲突）",
                    System.currentTimeMillis() - startedAt, username);
            throw new BusinessException(409, "用户名已存在，请更换后重试");
        }

        audit.record(OperationLogRecorder.MODULE_AUTH, OperationLogRecorder.ACTION_REGISTER,
                username + "（" + ROLE_DOCTOR + "）", true, null,
                System.currentTimeMillis() - startedAt, username);
        return new RegisterVO(entity.getId(), username, entity.getRealName(), entity.getRole());
    }
}
