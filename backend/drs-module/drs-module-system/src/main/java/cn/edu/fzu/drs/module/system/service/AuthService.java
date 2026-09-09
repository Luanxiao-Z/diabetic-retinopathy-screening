package cn.edu.fzu.drs.module.system.service;

import cn.edu.fzu.drs.module.system.dto.LoginDTO;
import cn.edu.fzu.drs.module.system.vo.LoginVO;

/**
 * 认证服务：登录创建令牌、登出销毁令牌。
 */
public interface AuthService {

    /**
     * 校验用户名/密码，成功返回登录结果（含令牌与权限集）。
     */
    LoginVO login(LoginDTO dto);

    /**
     * 登出：销毁令牌（token 为空则视为成功）。
     */
    void logout(String token);
}
