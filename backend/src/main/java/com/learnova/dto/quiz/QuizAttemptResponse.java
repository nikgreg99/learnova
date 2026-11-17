package com.learnova.dto.quiz;

import com.learnova.model.QuizAttemptStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response payload representing a quiz attempt")
public record QuizAttemptResponse(
        @Schema(description = "Unique ID of the attempt", example = "101") Long id,
        @Schema(description = "ID of the quiz", example = "5") Long quizId,
        @Schema(description = "Title of the quiz", example = "OOP Fundamentals Quiz") String quizTitle,
        @Schema(description = "ID of the student", example = "42") Long userId,
        @Schema(description = "Attempt number (1, 2, ...)", example = "1") Integer attemptNumber,
        @Schema(description = "Percentage score achieved", example = "85.00") BigDecimal score,
        @Schema(description = "Points earned", example = "17") Integer earnedPoints,
        @Schema(description = "Total possible points", example = "20") Integer totalPoints,
        @Schema(description = "Whether the passing threshold was achieved", example = "true") Boolean passed,
        @Schema(description = "Status of the attempt", example = "COMPLETED") QuizAttemptStatus status,
        @Schema(description = "Timestamp when the attempt started") LocalDateTime startedAt,
        @Schema(description = "Timestamp when the attempt was submitted/completed") LocalDateTime completedAt,
        @Schema(description = "Submitted answers summary") List<QuestionAnswerSubmission> answers) {
}
