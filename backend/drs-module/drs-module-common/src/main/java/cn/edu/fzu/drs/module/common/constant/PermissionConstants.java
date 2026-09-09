package cn.edu.fzu.drs.module.common.constant;

/**
 * 权限编码清单（前后端共用语义）。编码约定：{模块}:{页面}:{操作}，全小写冒号分隔。
 */
public final class PermissionConstants {

    public static final String COMMON_DICT_VIEW = "common:dict:view";

    public static final String ADMIN_USER_VIEW = "admin:user:view";
    public static final String ADMIN_USER_EDIT = "admin:user:edit";

    public static final String BIZ_SCREENING_CREATE = "biz:screening:create";
    public static final String BIZ_SCREENING_VIEW = "biz:screening:view";
    public static final String BIZ_SCREENING_EXPORT = "biz:screening:export";
    public static final String BIZ_SCREENING_DELETE = "biz:screening:delete";

    private PermissionConstants() {
    }
}
