package com.gsecp.system.controller;

import com.gsecp.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "系统")
@RestController
public class HealthController {

    @Operation(summary = "健康检查（无需登录）")
    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of("status", "UP", "version", "v0.2", "iteration", "1"));
    }

    @Operation(summary = "管理员权限校验示例")
    @GetMapping("/api/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> adminPing() {
        return R.ok("ADMIN 权限校验通过");
    }

    @Operation(summary = "教师权限校验示例")
    @GetMapping("/api/teacher/ping")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public R<String> teacherPing() {
        return R.ok("TEACHER 权限校验通过");
    }

    @Operation(summary = "学生权限校验示例")
    @GetMapping("/api/student/ping")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public R<String> studentPing() {
        return R.ok("STUDENT 权限校验通过");
    }
}
