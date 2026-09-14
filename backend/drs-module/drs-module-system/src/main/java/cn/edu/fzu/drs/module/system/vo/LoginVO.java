package cn.edu.fzu.drs.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录结果出参（含令牌与权限集，供前端构建菜单）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String token;
    private String userId;
    private String username;
    private String realName;
    private String role;
    private List<String> permissions;
    private String dataScope;
}
