package cn.edu.fzu.drs.module.common.exception;

/**
 * 业务异常：携带 code，由全局异常处理器转为统一响应体。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }
}
