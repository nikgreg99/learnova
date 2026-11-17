package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload representing an answer option for quiz-taking (zero-leak view)")
public record AnswerOptionTakeResponse(
        @Schema(description = "Unique ID of the answer option", example = "1") Long id,
        @Schema(description = "Text of the answer option", example = "Polymorphism") String text,
        @Schema(description = "Sequence position of the option (1-based)", example = "1") Integer position) {
}
