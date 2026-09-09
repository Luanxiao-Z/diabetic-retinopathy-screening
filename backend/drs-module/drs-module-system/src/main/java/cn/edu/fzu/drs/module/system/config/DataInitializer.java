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
 * 启动初始化：确保存在一个管理员账号（admin / admin123），便于首次登录与演示。
 * 数据库未就绪时静默跳过，不阻断应用启动。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_USERNAME = "admin";

    private final SysUserMapper userMapper;

    public DataInitializer(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, ADMIN_USERNAME));
            if (count != null && count > 0) {
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
        } catch (Exception e) {
            log.warn("初始化管理员账号失败（可能数据库尚未就绪）：{}", e.getMessage());
        }
    }
}
