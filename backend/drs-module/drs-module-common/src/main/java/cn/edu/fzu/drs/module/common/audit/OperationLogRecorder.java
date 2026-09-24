package cn.edu.fzu.drs.module.common.audit;

/**
 * 操作审计记录器（跨模块契约）。
 * <p>置于 common 模块，使 screening / system 等业务模块可在**不反向依赖 system 模块**的前提下
 * 记录审计日志；具体实现（落库）由 system 模块提供，运行期由 Spring 注入。</p>
 *
 * <p>约定：实现必须**吞掉自身异常**，审计失败不得影响主业务流程。</p>
 */
public interface OperationLogRecorder {

    /** 模块编码常量 */
    String MODULE_AUTH = "AUTH";
    String MODULE_SCREENING = "SCREENING";
    String MODULE_USER = "USER";
    String MODULE_DICT = "DICT";

    /** 动作编码常量 */
    String ACTION_LOGIN = "LOGIN";
    String ACTION_LOGOUT = "LOGOUT";
    String ACTION_UPLOAD = "UPLOAD";
    String ACTION_DELETE = "DELETE";
    String ACTION_EXPORT = "EXPORT";
    String ACTION_CREATE = "CREATE";
    String ACTION_UPDATE = "UPDATE";
    String ACTION_CHANGE_STATE = "CHANGE_STATE";
    String ACTION_REVIEW = "REVIEW";

    /**
     * 记录一次操作（操作人取当前登录主体）。
     *
     * @param module   模块编码，取值见 {@code MODULE_*} 常量
     * @param action   动作编码，取值见 {@code ACTION_*} 常量
     * @param target   操作对象描述（可为空）
     * @param success  是否成功
     * @param errorMsg 失败原因（成功时传 null）
     * @param costMs   耗时（毫秒）
     */
    void record(String module, String action, String target, boolean success, String errorMsg, long costMs);

    /**
     * 记录一次操作并显式指定操作人（用于登录等尚无会话的场景）。
     */
    void record(String module, String action, String target, boolean success, String errorMsg,
                long costMs, String username);
}
