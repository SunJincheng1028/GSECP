package com.gsecp.course;

import jakarta.validation.constraints.NotBlank;

public final class CourseDtos {

    private CourseDtos() {
    }

    public record CourseView(
            Long id,
            Long classId,
            String className,
            String name,
            String description,
            String status,
            String startAt,
            String endAt,
            Integer stepCount,
            String createdAt
    ) {
    }

    public record CreateCourseRequest(
            @NotBlank(message = "课程名称不能为空") String name,
            String description
    ) {
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }
}
