package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.exception.UnauthorizedException;
import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.common.util.PasswordUtil;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.system.dto.UserCreateDTO;
import cn.edu.fzu.drs.module.system.dto.UserPageQuery;
import cn.edu.fzu.drs.module.system.dto.UserUpdateDTO;
import cn.edu.fzu.drs.module.system.entity.SysUserEntity;
import cn.edu.fzu.drs.module.system.mapper.SysUserMapper;
import cn.edu.fzu.drs.module.system.service.AdminUserService;
import cn.edu.fzu.drs.module.system.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户管理实现。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final SysUserMapper userMapper;

    public AdminUserServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<UserVO> pageUsers(UserPageQuery query) {
        int current = (query.getCurrent() == null || query.getCurrent() < 1) ? 1 : query.getCurrent();
        int size = (query.getPageSize() == null || query.getPageSize() < 1) ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUserEntity::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getRole())) {
            wrapper.eq(SysUserEntity::getRole, query.getRole());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SysUserEntity::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysUserEntity::getCreateTime);
        IPage<SysUserEntity> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.convert(this::toVO));
    }

    @Override
    public UserVO detail(String id) {
        SysUserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVO(entity);
    }

    @Override
    public String createUser(UserCreateDTO dto) {
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword()) || dto.getPassword().length() < 6) {
            throw new BusinessException(400, "密码长度需为 6-64");
        }
        if (!"DOCTOR".equals(dto.getRole()) && !"ADMIN".equals(dto.getRole())) {
            throw new BusinessException(400, "角色仅支持 DOCTOR/ADMIN");
        }
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, dto.getUsername()));
        if (count != null && count > 0) {
            throw new BusinessException(409, "用户名已存在");
        }
        SysUserEntity entity = new SysUserEntity();
        entity.setUsername(dto.getUsername());
        entity.setRealName(dto.getRealName());
        entity.setRole(dto.getRole());
        entity.setPhone(dto.getPhone());
        entity.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : STATUS_ENABLED);
        entity.setPassword(PasswordUtil.encode(dto.getPassword()));
        // id / deleteFlag / createBy / updateBy 由 AuditMetaHandler 自动填充
        userMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public void updateUser(String id, UserUpdateDTO dto) {
        SysUserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (StringUtils.hasText(dto.getRealName())) {
            entity.setRealName(dto.getRealName());
        }
        if (StringUtils.hasText(dto.getRole())) {
            if (!"DOCTOR".equals(dto.getRole()) && !"ADMIN".equals(dto.getRole())) {
                throw new BusinessException(400, "角色仅支持 DOCTOR/ADMIN");
            }
            entity.setRole(dto.getRole());
        }
        if (dto.getPhone() != null) {
            entity.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            if (!STATUS_ENABLED.equals(dto.getStatus()) && !STATUS_DISABLED.equals(dto.getStatus())) {
                throw new BusinessException(400, "状态仅支持 ENABLED/DISABLED");
            }
            entity.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            if (dto.getPassword().length() < 6) {
                throw new BusinessException(400, "密码长度需为 6-64");
            }
            entity.setPassword(PasswordUtil.encode(dto.getPassword()));
        }
        userMapper.updateById(entity);
    }

    @Override
    public void deleteUser(String id) {
        String operator = currentUserId();
        if (id.equals(operator)) {
            throw new BusinessException(400, "不能删除当前登录账号");
        }
        SysUserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 全局逻辑删除配置：deleteById 自动置 deleteFlag=Y
        userMapper.deleteById(id);
    }

    @Override
    public void changeState(String id, String status) {
        String operator = currentUserId();
        if (id.equals(operator)) {
            throw new BusinessException(400, "不能操作当前登录账号状态");
        }
        if (!STATUS_ENABLED.equals(status) && !STATUS_DISABLED.equals(status)) {
            throw new BusinessException(400, "状态仅支持 ENABLED/DISABLED");
        }
        SysUserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        entity.setStatus(status);
        userMapper.updateById(entity);
    }

    private UserVO toVO(SysUserEntity entity) {
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setRole(entity.getRole());
        vo.setPhone(entity.getPhone());
        vo.setStatus(entity.getStatus());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    private String currentUserId() {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            throw new UnauthorizedException();
        }
        return principal.getUserId();
    }
}
