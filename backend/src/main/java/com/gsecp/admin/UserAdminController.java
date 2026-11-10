package com.gsecp.admin;

import com.gsecp.common.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService service;

    public UserAdminController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public R<List<UserAdminDtos.UserView>> list() {
        return R.ok(service.list());
    }

    @PostMapping
    public R<Void> create(@Valid @RequestBody UserAdminDtos.CreateUserRequest request) {
        service.create(request);
        return R.ok();
    }

    @PatchMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable long id,
                                @Valid @RequestBody UserAdminDtos.UpdateStatusRequest request) {
        service.updateStatus(id, request.status().toUpperCase());
        return R.ok();
    }

    @PostMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable long id,
                                 @Valid @RequestBody UserAdminDtos.ResetPasswordRequest request) {
        service.resetPassword(id, request.password());
        return R.ok();
    }
}
