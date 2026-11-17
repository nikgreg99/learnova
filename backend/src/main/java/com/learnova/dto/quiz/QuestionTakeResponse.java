package com.learnova.dto.quiz;

import com.learnova.model.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response payload representing a quiz question for students taking the quiz")
public record QuestionTakeResponse(
        @Schema(description = "Unique ID of the question", example = "1") Long id,
        @Schema(description = "ID of the parent quiz", example = "1") Long quizId,
        @Schema(description = "Question prompt text", example = "What is polymorphism in Java?") String text,
        @Schema(description = "Question type", example = "SINGLE_CHOICE") QuestionType type,
        @Schema(description = "Point value for this question", example = "1") Integer points,
        @Schema(description = "Sequence position of the question", example = "1") Integer position,
        @Schema(description = "List of answer options (without correctness indicators)") List<AnswerOptionTakeResponse> options) {
}
