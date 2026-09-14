package cn.edu.fzu.drs.module.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户视图对象：脱敏，不含密码等敏感字段。
 */
@Data
public class UserVO {

    private String id;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
