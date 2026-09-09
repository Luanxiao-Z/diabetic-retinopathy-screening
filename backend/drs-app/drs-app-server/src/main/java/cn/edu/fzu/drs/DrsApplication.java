package cn.edu.fzu.drs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端启动类。组件扫描根包为 cn.edu.fzu.drs，覆盖 common/security/system/screening 各模块。
 */
@SpringBootApplication
public class DrsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DrsApplication.class, args);
    }
}
