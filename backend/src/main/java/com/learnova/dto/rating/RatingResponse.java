package com.learnova.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response payload representing a rating")
public record RatingResponse(
        @Schema(description = "Unique identifier of the rating", example = "1") Long id,

        @Schema(description = "Identifier of the user who submitted the rating", example = "1") Long userId,

        @Schema(description = "Username of the reviewer", example = "test-student") String username,

        @Schema(description = "Identifier of the rated course, if applicable", example = "1") Long courseId,

        @Schema(description = "Identifier of the rated lesson, if applicable", example = "10") Long lessonId,

        @Schema(description = "Rating score from 1 to 5 stars", example = "5") Integer rating,

        @Schema(description = "Written feedback or review", example = "Great course, very comprehensive!") String review,

        @Schema(description = "Timestamp when the rating was created", example = "2026-11-10T12:00:00") LocalDateTime createdAt,

        @Schema(description = "Timestamp when the rating was last updated", example = "2026-11-10T12:30:00") LocalDateTime updatedAt) {
}
