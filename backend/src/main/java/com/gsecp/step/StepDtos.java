package com.gsecp.step;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class StepDtos {

    private StepDtos() {
    }

    public record DeliverableItemView(
            Long id,
            String name,
            String typeCode,
            String typeName,
            String frontendComponent,
            Boolean required,
            String constraintJson,
            Integer sortOrder
    ) {
    }

    public record StepView(
            Long id,
            Long courseId,
            String title,
            String description,
            String requirement,
            BigDecimal weight,
            String publishAt,
            String dueAt,
            String status,
            Boolean allowResubmit,
            Integer sortOrder,
            Integer submissionCount,
            String myStatus,
            List<DeliverableItemView> items
    ) {
    }

    public record DeliverableItemInput(
            String name,
            String typeCode,
            Boolean required,
            String constraintJson,
            Integer sortOrder
    ) {
    }

    public record StepSaveRequest(
            @NotBlank(message = "步骤标题不能为空") String title,
            String description,
            String requirement,
            BigDecimal weight,
            String publishAt,
            String dueAt,
            String status,
            Boolean allowResubmit,
            Integer sortOrder,
            List<DeliverableItemInput> items
    ) {
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }
}
