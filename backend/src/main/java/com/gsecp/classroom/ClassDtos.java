package com.gsecp.classroom;

import jakarta.validation.constraints.NotBlank;

public final class ClassDtos {

    private ClassDtos() {
    }

    public record ClassView(
            Long id,
            String name,
            String description,
            Long teacherId,
            String teacherName,
            String status,
            Integer studentCount,
            Integer courseCount,
            String createdAt
    ) {
    }

    public record CreateClassRequest(
            @NotBlank(message = "班级名称不能为空") String name,
            String description
    ) {
    }

    public record MemberView(
            Long studentId,
            String username,
            String realName,
            String userNo,
            String status,
            String joinedAt
    ) {
    }

    public record InviteCodeView(
            Long id,
            String code,
            String expireAt,
            Integer maxUses,
            Integer usedCount,
            Boolean enabled,
            String createdAt
    ) {
    }

    public record CreateInviteCodeRequest(Integer expireDays, Integer maxUses) {
    }

    public record UpdateInviteCodeRequest(Boolean enabled) {
    }
}
