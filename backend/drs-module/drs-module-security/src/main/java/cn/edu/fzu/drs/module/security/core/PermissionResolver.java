package cn.edu.fzu.drs.module.security.core;

import cn.edu.fzu.drs.module.common.constant.PermissionConstants;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 轻量化角色→权限映射（本期仅 DOCTOR / ADMIN 两角色，不建 RBAC 表）。
 * <ul>
 *   <li>DOCTOR：本人筛查记录的创建/查看、字典查看</li>
 *   <li>ADMIN：全量用户与筛查记录的管理、导出、删除</li>
 * </ul>
 * 数据权限：ADMIN=ALL，其余=SELF。
 */
@Component
public class PermissionResolver {

    private static final Set<String> DOCTOR_PERMISSIONS = Set.of(
            PermissionConstants.BIZ_SCREENING_CREATE,
            PermissionConstants.BIZ_SCREENING_VIEW,
            PermissionConstants.COMMON_DICT_VIEW
    );

    private static final Set<String> ADMIN_PERMISSIONS = Set.of(
            PermissionConstants.COMMON_DICT_VIEW,
            PermissionConstants.ADMIN_USER_VIEW,
            PermissionConstants.ADMIN_USER_EDIT,
            PermissionConstants.BIZ_SCREENING_CREATE,
            PermissionConstants.BIZ_SCREENING_VIEW,
            PermissionConstants.BIZ_SCREENING_EXPORT,
            PermissionConstants.BIZ_SCREENING_DELETE
    );

    public Set<String> resolve(String role) {
        if ("ADMIN".equals(role)) {
            return ADMIN_PERMISSIONS;
        }
        if ("DOCTOR".equals(role)) {
            return DOCTOR_PERMISSIONS;
        }
        return Set.of();
    }

    public String dataScope(String role) {
        return "ADMIN".equals(role) ? "ALL" : "SELF";
    }
}
