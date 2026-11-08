package com.learnova.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response payload representing a lesson")
public record LessonResponse(
        @Schema(description = "Unique identifier of the lesson", example = "1") Long id,
        @Schema(description = "ID of the course this lesson belongs to", example = "10") Long courseId,
        @Schema(description = "Title of the lesson", example = "Introduction to Spring Boot") String title,
        @Schema(description = "Brief summary of the lesson", example = "Overview of Spring Boot architecture") String description,
        @Schema(description = "Full text or markdown content of the lesson") String content,
        @Schema(description = "Sequence position within the course", example = "1") Integer position,
        @Schema(description = "Estimated duration in minutes", example = "15") Integer duration,
        @Schema(description = "Creation timestamp", example = "2026-11-06T12:00:00") LocalDateTime createdAt,
        @Schema(description = "Last update timestamp", example = "2026-11-06T12:30:00") LocalDateTime updatedAt) {
}
