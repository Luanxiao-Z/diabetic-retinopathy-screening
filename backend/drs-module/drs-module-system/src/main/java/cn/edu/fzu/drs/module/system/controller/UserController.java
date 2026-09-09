package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.security.context.AuthContext;
import cn.edu.fzu.drs.module.security.model.AuthPrincipal;
import cn.edu.fzu.drs.module.system.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前用户接口：返回登录主体的权限集与档案（仅需登录令牌）。
 */
@Tag(name = "当前用户", description = "登录主体自身的权限与档案")
@RestController
@RequestMapping("/api/v1/common/users/me")
public class UserController {

    @Operation(summary = "当前用户权限", description = "返回登录主体持有的权限编码列表")
    @GetMapping("/permissions")
    public Result<List<String>> permissions() {
        AuthPrincipal principal = AuthContext.get();
        return Result.ok(principal == null ? List.of() : new ArrayList<>(principal.getPermissions()));
    }

    @Operation(summary = "当前用户档案", description = "返回登录主体的基本信息与权限集")
    @GetMapping
    public Result<ProfileVO> profile() {
        AuthPrincipal principal = AuthContext.get();
        if (principal == null) {
            return Result.ok(null);
        }
        ProfileVO vo = new ProfileVO(principal.getUserId(), principal.getUsername(), null, principal.getRole(),
                new ArrayList<>(principal.getPermissions()), principal.getDataScope());
        return Result.ok(vo);
    }
}
