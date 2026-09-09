package cn.edu.fzu.drs.module.system.service.impl;

import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.util.PasswordUtil;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.security.service.TokenService;
import cn.edu.fzu.drs.module.system.dto.LoginDTO;
import cn.edu.fzu.drs.module.system.entity.SysUserEntity;
import cn.edu.fzu.drs.module.system.mapper.SysUserMapper;
import cn.edu.fzu.drs.module.system.service.AuthService;
import cn.edu.fzu.drs.module.system.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 认证服务实现：校验凭证后通过 {@link TokenService} 签发 UUID 令牌（Session 模式，存 Redis）。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final SysUserMapper userMapper;
    private final TokenService tokenService;

    public AuthServiceImpl(SysUserMapper userMapper, TokenService tokenService) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, dto.getUsername()));
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            throw new BusinessException(401, "账号已被停用");
        }
        String token = tokenService.createToken(user.getId(), user.getUsername(), user.getRole());
        AuthPrincipal principal = tokenService.getPrincipal(token);
        return new LoginVO(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole(),
                new ArrayList<>(principal.getPermissions()), principal.getDataScope());
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            tokenService.remove(token);
        }
    }
}
