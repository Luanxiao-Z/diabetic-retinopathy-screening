package cn.edu.fzu.drs.module.common.result;

/**
 * 主段状态码枚举，与 {@link Result} 的 code 字段对应。
 */
public enum ApiCode {

    SUCCESS(0, "success"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无访问权限"),
    PARAM_ERROR(400, "参数错误"),
    SYSTEM_ERROR(500, "系统错误");

    private final int code;
    private final String msg;

    ApiCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int code() {
        return code;
    }

    public String msg() {
        return msg;
    }
}
