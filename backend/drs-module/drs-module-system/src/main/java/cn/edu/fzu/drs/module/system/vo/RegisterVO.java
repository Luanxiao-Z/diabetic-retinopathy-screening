package cn.edu.fzu.drs.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册结果出参。
 * <p>注册成功不自动签发令牌，前端应引导用户回到登录页完成登录。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVO {

    private String id;
    private String username;
    private String realName;
    private String role;
}
