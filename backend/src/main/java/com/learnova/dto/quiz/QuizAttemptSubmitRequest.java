package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Request payload for submitting answers to a quiz attempt")
public record QuizAttemptSubmitRequest(
        @Schema(description = "List of answers submitted for each question") @NotNull @Valid List<QuestionAnswerSubmission> answers) {
}
