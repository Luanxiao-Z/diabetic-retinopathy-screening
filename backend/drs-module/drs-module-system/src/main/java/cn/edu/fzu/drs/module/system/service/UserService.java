package cn.edu.fzu.drs.module.system.service;

/**
 * 当前登录账号的自助操作。
 */
public interface UserService {

    /**
     * 修改当前登录账号密码：校验原密码一致后写入新密码（SHA-256 加盐）。
     */
    void changePassword(String oldPassword, String newPassword);
}
