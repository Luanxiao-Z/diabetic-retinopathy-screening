package cn.edu.fzu.drs.module.security.core;

/**
 * 数据权限类型（对齐《后端API权限控制规范》场景4）。
 * <p>本期实际启用 {@link #ALL}（管理员全量）/ {@link #SELF}（仅本人）；
 * 其余类型（部门、区域等）为预留，便于后续扩展。</p>
 */
public final class DataScopeType {

    private DataScopeType() {
    }

    /** 全部数据权限 */
    public static final String ALL = "ALL";

    /** 仅本人数据 */
    public static final String SELF = "SELF";

    /** 本部门 */
    public static final String DEPT = "DEPT";

    /** 本部门及子部门 */
    public static final String DEPT_AND_CHILD = "DEPT_AND_CHILD";

    /** 指定区域 */
    public static final String REGION = "REGION";

    /** 自定义 */
    public static final String CUSTOM = "CUSTOM";

    /**
     * 校验给定的数据权限类型是否合法。
     */
    public static boolean isValid(String scope) {
        return ALL.equals(scope) || SELF.equals(scope)
                || DEPT.equals(scope) || DEPT_AND_CHILD.equals(scope)
                || REGION.equals(scope) || CUSTOM.equals(scope);
    }
}
