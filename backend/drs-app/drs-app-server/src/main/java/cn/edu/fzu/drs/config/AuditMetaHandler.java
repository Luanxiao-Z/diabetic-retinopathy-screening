package cn.edu.fzu.drs.config;

import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 审计字段自动填充：
 * <ul>
 *   <li>插入时若 id 为空，自动填入 32 位 UUID（对齐 CHAR(32) 主键）；</li>
 *   <li>create_by / update_by 取当前登录主体（匿名场景留空，由库表允许 NULL）。</li>
 * </ul>
 * 时间字段（create_time / update_time）由数据库默认值与 ON UPDATE 维护，此处不覆盖。
 */
@Component
public class AuditMetaHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (getFieldValByName("id", metaObject) == null) {
            strictInsertFill(metaObject, "id", String.class, IdGenerator.nextId());
        }
        // 逻辑删除标记默认未删除（N），保证 MyBatis-Plus 逻辑删除过滤可见
        strictInsertFill(metaObject, "deleteFlag", String.class, "N");
        AuthPrincipal context = AuthContext.get();
        String operator = context == null ? null : context.getUserId();
        if (operator != null) {
            strictInsertFill(metaObject, "createBy", String.class, operator);
            strictInsertFill(metaObject, "updateBy", String.class, operator);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        AuthPrincipal context = AuthContext.get();
        String operator = context == null ? null : context.getUserId();
        if (operator != null) {
            strictUpdateFill(metaObject, "updateBy", String.class, operator);
        }
    }
}
