package cn.edu.fzu.drs.module.system.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前登录用户档案出参（GET /api/v1/common/users/me）。
 */
@Data
@NoArgsConstructor
public class ProfileVO {

    private String userId;
    private String username;
    private String realName;
    private String role;
    private List<String> permissions;
    private String dataScope;

    /** 手机号（可为空） */
    private String phone;

    /** 账号创建时间 */
    private LocalDateTime createTime;
}
