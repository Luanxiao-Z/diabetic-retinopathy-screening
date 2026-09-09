package cn.edu.fzu.drs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端启动类。组件扫描根包为 cn.edu.fzu.drs，覆盖 common/security/system/screening 各模块。
 * Mapper 接口位于各业务模块的 mapper 包，统一在此扫描注册。
 */
@SpringBootApplication
@MapperScan({
        "cn.edu.fzu.drs.module.system.mapper",
        "cn.edu.fzu.drs.module.screening.mapper"
})
public class DrsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrsApplication.class, args);
    }
}
