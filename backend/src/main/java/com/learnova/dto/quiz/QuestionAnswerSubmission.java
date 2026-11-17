package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Answer submission for a single question")
public record QuestionAnswerSubmission(
        @Schema(description = "ID of the question being answered", example = "1") @NotNull Long questionId,
        @Schema(description = "Selected option IDs", example = "[10, 11]") @NotNull List<Long> selectedOptionIds) {
}
