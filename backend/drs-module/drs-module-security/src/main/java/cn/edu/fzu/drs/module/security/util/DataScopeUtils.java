package cn.edu.fzu.drs.module.security.util;

import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.core.DataScopeType;

/**
 * 数据权限工具：从当前请求线程的登录主体读取数据权限范围。
 * <p>业务层（场景4 的 biz 接口）据此拼接数据过滤条件，实现 {@link DataScopeType#SELF} / {@link DataScopeType#ALL} 隔离。</p>
 */
public final class DataScopeUtils {

    private DataScopeUtils() {
    }

    /**
     * 当前用户数据权限类型。未登录或缺失时默认 {@link DataScopeType#SELF}（最小权限原则）。
     */
    public static String getCurrentDataScope() {
        var principal = AuthContext.get();
        if (principal == null || principal.getDataScope() == null) {
            return DataScopeType.SELF;
        }
        return principal.getDataScope();
    }

    /**
     * 是否为全量数据权限。
     */
    public static boolean isAll() {
        return DataScopeType.ALL.equals(getCurrentDataScope());
    }

    /**
     * 是否为仅本人数据权限。
     */
    public static boolean isSelf() {
        return DataScopeType.SELF.equals(getCurrentDataScope());
    }
}
