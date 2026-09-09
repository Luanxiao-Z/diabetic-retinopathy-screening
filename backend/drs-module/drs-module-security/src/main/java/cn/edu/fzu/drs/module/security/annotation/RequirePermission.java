package cn.edu.fzu.drs.module.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 功能权限注解。编码约定：{模块}:{页面}:{操作}，全小写冒号分隔（增删改合并为 edit）。
 * <p>示例：@RequirePermission("biz:screening:edit")</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    String value();
}
