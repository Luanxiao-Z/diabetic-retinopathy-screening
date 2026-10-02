package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.audit.OperationLogRecorder;
import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.common.util.PasswordUtil;
import cn.edu.fzu.drs.module.system.dto.ProfileUpdateDTO;
import cn.edu.fzu.drs.module.system.entity.SysUserEntity;
import cn.edu.fzu.drs.module.system.mapper.SysUserMapper;
import cn.edu.fzu.drs.module.system.service.UserService;
import cn.edu.fzu.drs.module.system.vo.ProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

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

    @Override
    public ProfileVO currentProfile() {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || principal.getUsername() == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, principal.getUsername()));

        ProfileVO vo = new ProfileVO();
        vo.setUserId(principal.getUserId());
        vo.setUsername(principal.getUsername());
        vo.setRole(principal.getRole());
        // 权限集与数据范围来自登录主体（角色映射结果），无需查库
        vo.setPermissions(new ArrayList<>(principal.getPermissions()));
        vo.setDataScope(principal.getDataScope());
        if (user != null) {
            vo.setRealName(user.getRealName());
            vo.setPhone(user.getPhone());
            vo.setCreateTime(user.getCreateTime());
        }
        return vo;
    }

    @Override
    public ProfileVO updateProfile(ProfileUpdateDTO dto) {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null || principal.getUsername() == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, principal.getUsername()));
        if (user == null) {
            throw new BusinessException(404, "账号不存在");
        }
        // 仅更新资料字段；角色、状态、数据范围等权限相关字段不在此接口开放
        //
        // 注意：此处不能用 updateById —— MyBatis-Plus 默认字段策略为 NOT_NULL，
        // 会把 null 字段排除在 SET 子句之外，导致「清空手机号」无法生效。
        // 改用 LambdaUpdateWrapper 显式 set，保证 null 也能写入。
        LambdaUpdateWrapper<SysUserEntity> update = new LambdaUpdateWrapper<>();
        update.eq(SysUserEntity::getId, user.getId())
                .set(SysUserEntity::getRealName, dto.getRealName().trim())
                .set(SysUserEntity::getPhone, StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        userMapper.update(null, update);

        audit.record(OperationLogRecorder.MODULE_USER, OperationLogRecorder.ACTION_UPDATE,
                principal.getUsername() + "（本人资料）", true, null, 0L, principal.getUsername());

        return currentProfile();
    }
}
