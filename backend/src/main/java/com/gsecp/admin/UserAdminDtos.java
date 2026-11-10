package com.gsecp.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserAdminDtos {

    private UserAdminDtos() {
    }

    public record UserView(
            Long id,
            String username,
            String realName,
            String userNo,
            String role,
            String status,
            String createdAt
    ) {
    }

    public record CreateUserRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 64) String password,
            @NotBlank(message = "姓名不能为空") String realName,
            String userNo,
            @NotBlank(message = "角色不能为空") String role
    ) {
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(min = 6, max = 64) String password
    ) {
    }
}
