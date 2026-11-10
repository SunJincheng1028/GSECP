package com.gsecp.submission;

import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public final class SubmissionDtos {

    private SubmissionDtos() {
    }

    public record SubmissionItemInput(
            Long deliverableItemId,
            String textValue,
            Long fileId,
            String urlValue
    ) {
    }

    public record SubmitRequest(
            @NotEmpty(message = "提交内容不能为空") List<SubmissionItemInput> items
    ) {
    }

    public record SubmissionItemView(
            Long id,
            Long deliverableItemId,
            String name,
            String typeCode,
            String typeName,
            String frontendComponent,
            Boolean required,
            String textValue,
            Long fileId,
            String originalName,
            String urlValue
    ) {
    }

    public record SubmissionView(
            Long id,
            Long stepId,
            Long studentId,
            String studentName,
            Integer version,
            String status,
            Boolean latest,
            String submitAt,
            BigDecimal score,
            String comment,
            List<SubmissionItemView> items
    ) {
    }

    public record ProgressView(
            Long studentId,
            String username,
            String realName,
            String userNo,
            Long submissionId,
            String status,
            Integer version,
            String submitAt,
            BigDecimal score,
            String comment
    ) {
    }
}
