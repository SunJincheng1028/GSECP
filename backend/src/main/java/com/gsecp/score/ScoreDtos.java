package com.gsecp.score;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ScoreDtos {

    private ScoreDtos() {
    }

    public record GradeRequest(
            @NotNull(message = "分数不能为空") BigDecimal totalScore,
            String comment
    ) {
    }

    public record StepColumn(Long stepId, String title, BigDecimal weight) {
    }

    public record StepGrade(
            Long stepId,
            String title,
            BigDecimal weight,
            BigDecimal score,
            String comment,
            Long submissionId
    ) {
    }

    public record StudentGradeRow(
            Long studentId,
            String username,
            String realName,
            String userNo,
            List<StepGrade> steps,
            BigDecimal totalScore,
            BigDecimal gradedWeight
    ) {
    }

    public record GradeReport(
            Long courseId,
            String courseName,
            List<StepColumn> columns,
            List<StudentGradeRow> rows
    ) {
    }
}
