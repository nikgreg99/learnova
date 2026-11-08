package com.learnova.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request payload for partially updating a lesson")
public record LessonPatchRequest(
        @Schema(description = "Title of the lesson", example = "Dependency Injection") String title,
        @Schema(description = "Description of the lesson", example = "Overview of IoC and DI") String description,
        @Schema(description = "Content or markdown of the lesson", example = "# Dependency Injection\n...") String content,
        @Schema(description = "Position/order of the lesson within the course", example = "2") @Positive Integer position,
        @Schema(description = "Duration of the lesson in minutes", example = "25") @PositiveOrZero Integer duration) {
}
