package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.constant.AuthConstants;
import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.system.dto.LoginDTO;
import cn.edu.fzu.drs.module.system.service.AuthService;
import cn.edu.fzu.drs.module.system.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录（匿名）、登出。
 */
@Tag(name = "认证管理", description = "登录会话的创建与销毁")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "校验用户名/密码，签发 UUID 令牌（X-Access-Token）")
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
}
