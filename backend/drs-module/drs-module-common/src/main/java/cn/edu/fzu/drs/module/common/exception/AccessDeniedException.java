package cn.edu.fzu.drs.module.common.exception;

/**
 * 无访问权限异常，由全局异常处理器映射为 403。
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("无访问权限");
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
