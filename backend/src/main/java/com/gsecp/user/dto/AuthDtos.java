package com.gsecp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record UserInfo(
            Long userId,
            String username,
            String realName,
            String userNo,
            List<String> roles
    ) {
    }

    public record LoginResponse(
            String token,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64, message = "密码长度需为 6~64 位") String password,
            @NotBlank(message = "姓名不能为空") String realName,
            String userNo,
            @NotBlank(message = "邀请码不能为空") String inviteCode
    ) {
    }
}
