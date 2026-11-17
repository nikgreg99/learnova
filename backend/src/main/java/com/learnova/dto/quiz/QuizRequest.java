package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload for creating a quiz")
public record QuizRequest(
        @Schema(description = "Title of the quiz", example = "OOP Fundamentals Quiz") @NotBlank String title,
        @Schema(description = "Description or instructions for the quiz") String description,
        @Schema(description = "Minimum score required to pass (percentage 0-100)", example = "70") @NotNull @Min(0) @Max(100) Integer passingScore,
        @Schema(description = "Maximum attempts allowed (null for unlimited)", example = "3") @Positive Integer maxAttempts,
        @Schema(description = "Time limit in minutes (null for unlimited)", example = "30") @Positive Integer timeLimitMinutes,
        @Schema(description = "Whether the quiz is active and accessible to enrolled students", example = "true") Boolean isActive) {
}
