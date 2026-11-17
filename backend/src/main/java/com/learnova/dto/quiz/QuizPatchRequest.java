package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request payload for partially updating a quiz")
public record QuizPatchRequest(
        @Schema(description = "Title of the quiz") String title,
        @Schema(description = "Description or instructions for the quiz") String description,
        @Schema(description = "Minimum score required to pass (percentage 0-100)") @Min(0) @Max(100) Integer passingScore,
        @Schema(description = "Maximum attempts allowed (null for unlimited)") @Positive Integer maxAttempts,
        @Schema(description = "Time limit in minutes (null for unlimited)") @Positive Integer timeLimitMinutes,
        @Schema(description = "Whether the quiz is active") Boolean isActive) {
}
