package cn.edu.fzu.drs.module.common.util;

import java.util.UUID;

/**
 * 主键生成器：返回 32 位无连字符 UUID（对齐数据库设计规范 CHAR(32) 主键）。
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
