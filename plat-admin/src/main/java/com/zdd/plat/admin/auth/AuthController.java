package com.zdd.plat.admin.auth;

import com.zdd.plat.admin.auth.dto.LoginRequest;
import com.zdd.plat.admin.auth.dto.RefreshRequest;
import com.zdd.plat.admin.auth.dto.RegisterRequest;
import com.zdd.plat.admin.auth.dto.TokenResponse;
import com.zdd.plat.admin.common.api.Result;
import com.zdd.plat.admin.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "开发者注册")
    @PostMapping("/register")
    public Result<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录（开发者/管理员）")
    @PostMapping("/login")
    public Result<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public Result<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @Operation(summary = "当前登录用户信息")
    @GetMapping("/me")
    public Result<TokenResponse> me() {
        return Result.ok(authService.me(CurrentUser.get()));
    }
}
