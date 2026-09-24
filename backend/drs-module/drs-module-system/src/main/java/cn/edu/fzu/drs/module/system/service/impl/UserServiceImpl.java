package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.audit.OperationLogRecorder;
import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.common.util.PasswordUtil;
import cn.edu.fzu.drs.module.system.entity.SysUserEntity;
import cn.edu.fzu.drs.module.system.mapper.SysUserMapper;
import cn.edu.fzu.drs.module.system.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

/**
 * 当前登录账号自助操作实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final OperationLogRecorder audit;

    public UserServiceImpl(SysUserMapper userMapper, OperationLogRecorder audit) {
        this.userMapper = userMapper;
        this.audit = audit;
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || principal.getUsername() == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, principal.getUsername()));
        if (user == null) {
            throw new BusinessException(404, "账号不存在");
        }
        if (!PasswordUtil.matches(oldPassword, user.getPassword())) {
            audit.record(OperationLogRecorder.MODULE_AUTH, "CHANGE_PASSWORD",
                    principal.getUsername(), false, "原密码校验失败", 0L, principal.getUsername());
            throw new BusinessException(400, "原密码不正确");
        }
        if (PasswordUtil.matches(newPassword, user.getPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        user.setPassword(PasswordUtil.encode(newPassword));
        userMapper.updateById(user);

        audit.record(OperationLogRecorder.MODULE_AUTH, "CHANGE_PASSWORD",
                principal.getUsername(), true, null, 0L, principal.getUsername());
    }
}
