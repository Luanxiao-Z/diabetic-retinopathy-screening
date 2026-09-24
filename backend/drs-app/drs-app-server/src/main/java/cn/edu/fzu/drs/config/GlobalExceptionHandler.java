package cn.edu.fzu.drs.config;

import cn.edu.fzu.drs.module.common.exception.AccessDeniedException;
import cn.edu.fzu.drs.module.common.exception.BusinessException;
import cn.edu.fzu.drs.module.common.exception.UnauthorizedException;
import cn.edu.fzu.drs.module.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：将业务/认证/权限/校验/路由异常统一封装为 {@link Result}。
 * <p>认证/权限异常同时设置对应 HTTP 状态码（401/403），与 AuthFilter 行为保持一致；
 * 参数校验失败返回 400 与具体字段提示；路径不存在返回 404（而非 500）。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result<Void>> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.fail(401, e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleForbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.fail(403, e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 请求体字段校验失败（@Valid）。
     * <p>返回 400 与**首个字段的具体提示**，而非笼统的「系统错误」——否则前端表单
     * 无法给出有效反馈（例如「新密码长度需为 6-64」）。</p>
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result<Void>> handleValidation(Exception e) {
        String message = "请求参数校验失败";
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().hasFieldErrors()) {
            FieldError first = ex.getBindingResult().getFieldError();
            message = first == null ? message : first.getDefaultMessage();
        } else if (e instanceof BindException ex && ex.getBindingResult().hasFieldErrors()) {
            FieldError first = ex.getBindingResult().getFieldError();
            message = first == null ? message : first.getDefaultMessage();
        }
        return ResponseEntity.status(HttpStatus.OK).body(Result.fail(400, message));
    }

    /**
     * 请求路径不存在（或路径变量缺失导致落到静态资源处理器）。
     * <p>返回 404，避免把「路由不存在」误报为 500 系统错误。</p>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(404, "请求的接口不存在"));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未处理的系统异常", e);
        return Result.fail(500, "系统错误");
    }
}
