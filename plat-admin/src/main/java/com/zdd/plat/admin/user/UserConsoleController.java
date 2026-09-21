package com.zdd.plat.admin.user;

import com.zdd.plat.admin.common.api.PageQuery;
import com.zdd.plat.admin.common.api.PageResult;
import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.common.security.CurrentUser;
import com.zdd.plat.admin.user.UserService.PasswordResetVO;
import com.zdd.plat.admin.user.UserService.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营侧用户管理（仅 ADMIN）。
 */
@Tag(name = "运营-用户管理")
@RestController
@RequestMapping("/api/console/users")
@RequiredArgsConstructor
public class UserConsoleController {

    private final UserService userService;

    @Operation(summary = "用户分页（可按角色过滤）")
    @GetMapping
    public Result<PageResult<UserVO>> page(PageQuery query,
                                           @RequestParam(required = false) String role) {
        return Result.ok(userService.page(query, role));
    }

    @Operation(summary = "启用/禁用用户（1-启用 0-禁用）")
    @PostMapping("/{id}/status")
    public Result<UserVO> updateStatus(@PathVariable Long id, @RequestParam int status) {
        return Result.ok(userService.updateStatus(CurrentUser.get(), id, status));
    }

    @Operation(summary = "重置用户密码（返回一次性新密码）")
    @PostMapping("/{id}/reset-password")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long id) {
        return Result.ok(userService.resetPassword(id));
    }
}
