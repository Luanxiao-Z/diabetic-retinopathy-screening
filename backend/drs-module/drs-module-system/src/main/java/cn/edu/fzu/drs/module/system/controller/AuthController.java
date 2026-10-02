package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.constant.AuthConstants;
import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.system.dto.LoginDTO;
import cn.edu.fzu.drs.module.system.dto.RegisterDTO;
import cn.edu.fzu.drs.module.system.service.AuthService;
import cn.edu.fzu.drs.module.system.service.CaptchaService;
import cn.edu.fzu.drs.module.system.vo.CaptchaVO;
import cn.edu.fzu.drs.module.system.vo.LoginVO;
import cn.edu.fzu.drs.module.system.vo.RegisterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录（匿名）、登出、自助注册（匿名）、图形验证码（匿名）。
 * <p>本控制器全部路径位于 {@link AuthConstants#ANONYMOUS_PREFIX} 之下，由认证过滤器直接放行。</p>
 */
@Tag(name = "认证管理", description = "登录会话的创建与销毁、账号自助注册、图形验证码")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(AuthService authService, CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @Operation(summary = "图形验证码", description = "生成一张字符验证码；文本存服务端 Redis，5 分钟有效且校验后立即失效")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        return Result.ok(captchaService.generate());
    }

    @Operation(summary = "登录", description = "校验验证码与用户名/密码，签发 UUID 令牌（X-Access-Token）")
    @PostMapping("/sessions")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @Operation(summary = "登出", description = "销毁当前令牌；匿名路径，令牌缺失视为成功")
    @DeleteMapping("/sessions")
    public Result<Void> logout(@RequestHeader(value = AuthConstants.TOKEN_HEADER, required = false) String token) {
        authService.logout(token);
        return Result.ok();
    }

    @Operation(summary = "注册", description = "自助注册普通医生账号（DOCTOR / 仅本人数据）；受 IP 与全局双层频率限制")
    @PostMapping("/users")
    public Result<RegisterVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        return Result.ok(authService.register(dto, clientIp(request)));
    }

    /**
     * 提取客户端 IP。优先取反向代理头，便于部署在 Nginx 之后时仍能正确限流。
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 可能是「客户端, 代理1, 代理2」，取第一段
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
