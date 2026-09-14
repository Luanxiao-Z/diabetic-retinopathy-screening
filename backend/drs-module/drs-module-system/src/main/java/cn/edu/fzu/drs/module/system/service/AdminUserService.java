package cn.edu.fzu.drs.module.system.service;

import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.system.dto.UserCreateDTO;
import cn.edu.fzu.drs.module.system.dto.UserPageQuery;
import cn.edu.fzu.drs.module.system.dto.UserUpdateDTO;
import cn.edu.fzu.drs.module.system.vo.UserVO;

/**
 * 用户管理（场景3：需 admin:user:* 权限）。
 */
public interface AdminUserService {

    /**
     * 分页查询用户（逻辑删除记录已自动过滤）。
     */
    PageResult<UserVO> pageUsers(UserPageQuery query);

    /**
     * 用户详情。
     */
    UserVO detail(String id);

    /**
     * 新增用户（密码加密存储）。返回新用户 ID。
     */
    String createUser(UserCreateDTO dto);

    /**
     * 修改用户（全量更新，未提供字段不覆盖）。
     */
    void updateUser(String id, UserUpdateDTO dto);

    /**
     * 删除用户（逻辑删除）。禁止删除当前登录账号。
     */
    void deleteUser(String id);

    /**
     * 启用/禁用用户。禁止操作当前登录账号。
     */
    void changeState(String id, String status);
}
