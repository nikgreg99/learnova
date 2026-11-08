package com.learnova.dto.course;

import com.learnova.model.CourseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for updating course publication status")
public record CourseStatusUpdateRequest(
        @Schema(description = "The target publication status", example = "PUBLISHED") @NotNull CourseStatus status) {
}
