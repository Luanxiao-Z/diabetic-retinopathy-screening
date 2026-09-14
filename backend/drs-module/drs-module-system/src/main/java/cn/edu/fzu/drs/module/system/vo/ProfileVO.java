package cn.edu.fzu.drs.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前登录用户档案出参（GET /api/v1/common/users/me）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileVO {

    private String userId;
    private String username;
    private String realName;
    private String role;
    private List<String> permissions;
    private String dataScope;
}
