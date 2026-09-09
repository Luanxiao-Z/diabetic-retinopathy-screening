package cn.edu.fzu.drs.module.system.entity;

import cn.edu.fzu.drs.module.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统用户实体（对应表 sys_user）。
 */
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** 登录用户名（字典:C_USER_ROLE 之外，唯一编码，区分大小写） */
    private String username;

    /** 密码（SHA-256 + 随机盐，格式 salt:hash） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色（字典:B_USER_ROLE）DOCTOR/ADMIN */
    private String role;

    /** 手机号 */
    private String phone;

    /** 状态（字典:C_STATUS）ENABLED/DISABLED */
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
