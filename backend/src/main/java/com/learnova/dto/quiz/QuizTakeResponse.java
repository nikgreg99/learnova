package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response payload for students taking a quiz (zero-leak view)")
public record QuizTakeResponse(
        @Schema(description = "Unique ID of the quiz", example = "1") Long id,
        @Schema(description = "ID of the parent course", example = "10") Long courseId,
        @Schema(description = "Title of the quiz", example = "OOP Fundamentals Quiz") String title,
        @Schema(description = "Description of the quiz") String description,
        @Schema(description = "Passing score percentage", example = "70") Integer passingScore,
        @Schema(description = "Maximum attempts allowed", example = "3") Integer maxAttempts,
        @Schema(description = "Time limit in minutes", example = "30") Integer timeLimitMinutes,
        @Schema(description = "List of questions without correct answer indicators") List<QuestionTakeResponse> questions) {
}
