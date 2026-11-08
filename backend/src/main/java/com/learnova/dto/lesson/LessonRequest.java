package com.learnova.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request payload for creating or updating a lesson")
public record LessonRequest(
        @Schema(description = "Title of the lesson", example = "Introduction to Spring Boot") @NotBlank String title,
        @Schema(description = "Summary of the lesson", example = "Overview of Spring Boot architecture") String description,
        @Schema(description = "Full text content of the lesson") String content,
        @Schema(description = "Sequence position of the lesson within the course (1-based)", example = "1") @NotNull(message = "Position is required") @Min(value = 1, message = "Position must be greater than or equal to 1") Integer position,
        @Schema(description = "Estimated duration in minutes", example = "15") @PositiveOrZero(message = "Duration must be positive or zero") Integer duration) {
}
