package com.gsecp.user.controller;

import com.gsecp.common.R;
import com.gsecp.security.UserPrincipal;
import com.gsecp.user.dto.AuthDtos;
import com.gsecp.user.service.AuthService;
import com.gsecp.user.service.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RegisterService registerService;

    public AuthController(AuthService authService, RegisterService registerService) {
        this.authService = authService;
        this.registerService = registerService;
    }

    @Operation(summary = "学生凭邀请码注册进班，并自动登录")
    @PostMapping("/register")
    public R<AuthDtos.LoginResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return R.ok(registerService.register(request));
    }

    @Operation(summary = "登录，返回 JWT 与用户信息")
    @PostMapping("/login")
    public R<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "登出（无状态，前端移除 Token 即可）")
    @PostMapping("/logout")
    public R<Void> logout() {
        return R.ok();
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public R<AuthDtos.UserInfo> me(@AuthenticationPrincipal UserPrincipal principal) {
        return R.ok(authService.currentUser(principal));
    }
}
