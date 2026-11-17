package com.learnova.dto.quiz;

import com.learnova.model.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Request payload for creating or updating a quiz question")
public record QuestionRequest(
        @Schema(description = "Question prompt text", example = "What is polymorphism in Java?") @NotBlank String text,
        @Schema(description = "Question type", example = "SINGLE_CHOICE") @NotNull QuestionType type,
        @Schema(description = "Point value for this question", example = "1") @NotNull @Min(1) Integer points,
        @Schema(description = "Sequence position of the question (1-based)", example = "1") @NotNull @Min(1) Integer position,
        @Schema(description = "List of answer options") @NotEmpty @Valid List<AnswerOptionRequest> options) {
}
