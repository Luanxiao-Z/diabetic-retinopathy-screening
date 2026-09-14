package cn.edu.fzu.drs.module.system.config;

import cn.edu.fzu.drs.module.common.util.IdGenerator;
import cn.edu.fzu.drs.module.common.util.PasswordUtil;
import cn.edu.fzu.drs.module.system.entity.SysUserEntity;
import cn.edu.fzu.drs.module.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动初始化：确保存在演示账号，便于首次登录与权限演示。
 * <ul>
 *   <li>admin / admin123 —— 角色 ADMIN（拥有全部权限，数据范围 ALL）</li>
 *   <li>doctor / doctor123 —— 角色 DOCTOR（仅业务与字典查看权限，数据范围 SELF，可演示 403）</li>
 * </ul>
 * 数据库未就绪时静默跳过，不阻断应用启动。每个账号独立判断是否已存在，互不影响。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String DOCTOR_USERNAME = "doctor";

    private final SysUserMapper userMapper;

    public DataInitializer(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedAdmin();
            seedDoctor();
        } catch (Exception e) {
            log.warn("初始化演示账号失败（可能数据库尚未就绪）：{}", e.getMessage());
        }
    }

    private void seedAdmin() {
        if (exists(ADMIN_USERNAME)) {
            return;
        }
        SysUserEntity admin = new SysUserEntity();
        admin.setId(IdGenerator.nextId());
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(PasswordUtil.encode("admin123"));
        admin.setRealName("系统管理员");
        admin.setRole("ADMIN");
        admin.setStatus("ENABLED");
        admin.setDeleteFlag("N");
        userMapper.insert(admin);
        log.info("已初始化管理员账号 {} / admin123", ADMIN_USERNAME);
    }

    private void seedDoctor() {
        if (exists(DOCTOR_USERNAME)) {
            return;
        }
        SysUserEntity doctor = new SysUserEntity();
        doctor.setId(IdGenerator.nextId());
        doctor.setUsername(DOCTOR_USERNAME);
        doctor.setPassword(PasswordUtil.encode("doctor123"));
        doctor.setRealName("演示医生");
        doctor.setRole("DOCTOR");
        doctor.setStatus("ENABLED");
        doctor.setDeleteFlag("N");
        userMapper.insert(doctor);
        log.info("已初始化医生账号 {} / doctor123", DOCTOR_USERNAME);
    }

    private boolean exists(String username) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, username));
        return count != null && count > 0;
    }
}
