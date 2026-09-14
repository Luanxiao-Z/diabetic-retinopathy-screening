package cn.edu.fzu.drs.module.system.controller;

import cn.edu.fzu.drs.module.common.result.PageResult;
import cn.edu.fzu.drs.module.common.result.Result;
import cn.edu.fzu.drs.module.security.annotation.RequirePermission;
import cn.edu.fzu.drs.module.system.dto.UserCreateDTO;
import cn.edu.fzu.drs.module.system.dto.UserPageQuery;
import cn.edu.fzu.drs.module.system.dto.UserUpdateDTO;
import cn.edu.fzu.drs.module.system.service.AdminUserService;
import cn.edu.fzu.drs.module.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（场景3：认证 + 功能权限 admin:user:*）。
 */
@Tag(name = "用户管理", description = "系统用户的查询与维护（需 admin:user:* 权限）")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {

    private final AdminUserService userService;

    public AdminUserController(AdminUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户", description = "支持按用户名模糊、角色、状态过滤")
    @GetMapping("/users")
    @RequirePermission("admin:user:view")
    public Result<PageResult<UserVO>> pageUsers(UserPageQuery query) {
        return Result.ok(userService.pageUsers(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/users/{id}")
    @RequirePermission("admin:user:view")
    public Result<UserVO> detail(@PathVariable("id") String id) {
        return Result.ok(userService.detail(id));
    }

    @Operation(summary = "新增用户", description = "密码明文入参，存储时 SHA-256 加盐加密")
    @PostMapping("/users")
    @RequirePermission("admin:user:edit")
    public ResponseEntity<Result<String>> create(@Valid @RequestBody UserCreateDTO dto) {
        String id = userService.createUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.ok(id));
    }

    @Operation(summary = "修改用户", description = "全量更新，未提供字段不覆盖")
    @PutMapping("/users/{id}")
    @RequirePermission("admin:user:edit")
    public Result<Void> update(@PathVariable("id") String id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除用户", description = "逻辑删除，禁止删除当前登录账号")
    @DeleteMapping("/users/{id}")
    @RequirePermission("admin:user:edit")
    public Result<Void> delete(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return Result.ok();
    }

    @Operation(summary = "启用/禁用用户", description = "status=ENABLED|DISABLED，禁止操作当前登录账号")
    @PatchMapping("/users/{id}/state")
    @RequirePermission("admin:user:edit")
    public Result<Void> changeState(@PathVariable("id") String id, @RequestParam("status") String status) {
        userService.changeState(id, status);
        return Result.ok();
    }
}
