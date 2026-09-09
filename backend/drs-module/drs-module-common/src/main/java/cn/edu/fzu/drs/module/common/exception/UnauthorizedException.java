package cn.edu.fzu.drs.module.common.exception;

/**
 * 未登录/登录过期异常，由全局异常处理器映射为 401。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("未登录或登录已过期");
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
