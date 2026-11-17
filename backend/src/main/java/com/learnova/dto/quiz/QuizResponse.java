package com.learnova.dto.quiz;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Response payload representing a quiz (instructor / admin view)")
public record QuizResponse(
        @Schema(description = "Unique ID of the quiz", example = "1") Long id,
        @Schema(description = "ID of the parent course", example = "10") Long courseId,
        @Schema(description = "Title of the quiz", example = "OOP Fundamentals Quiz") String title,
        @Schema(description = "Description of the quiz") String description,
        @Schema(description = "Passing score percentage", example = "70") Integer passingScore,
        @Schema(description = "Maximum attempts allowed", example = "3") Integer maxAttempts,
        @Schema(description = "Time limit in minutes", example = "30") Integer timeLimitMinutes,
        @Schema(description = "Whether the quiz is active", example = "true") Boolean isActive,
        @Schema(description = "List of questions with answers") List<QuestionResponse> questions,
        @Schema(description = "Creation timestamp") LocalDateTime createdAt,
        @Schema(description = "Last update timestamp") LocalDateTime updatedAt) {
}
