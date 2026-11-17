package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for creating or updating an answer option")
public record AnswerOptionRequest(
        @Schema(description = "Text of the answer option", example = "Polymorphism") @NotBlank String text,
        @Schema(description = "Whether this option is correct", example = "true") @NotNull Boolean isCorrect,
        @Schema(description = "Sequence position of the option (1-based)", example = "1") @NotNull @Min(1) Integer position) {
}
