package cn.edu.fzu.drs.module.system.vo;

import java.util.List;

/**
 * 当前登录用户档案出参（GET /api/v1/common/users/me）。
 */
public class ProfileVO {

    private String userId;
    private String username;
    private String realName;
    private String role;
    private List<String> permissions;
    private String dataScope;

    public ProfileVO() {
    }

    public ProfileVO(String userId, String username, String realName, String role,
                      List<String> permissions, String dataScope) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.permissions = permissions;
        this.dataScope = dataScope;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }
}
