package com.learnova.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Response payload representing a course")
public record CourseResponse(
        @Schema(description = "Unique identifier of the course", example = "1") Long id,
        @Schema(description = "Name of the course", example = "Spring Boot") @NotBlank String name,
        @Schema(description = "Description of the course", example = "Comprehensive guide to master Spring boot") String description,
        @Schema(description = "Price of the course", example = "49.99") @PositiveOrZero @NotNull BigDecimal price,
        @Schema(description = "Timestamp when the course was created", example = "2026-10-31T12:00:00") LocalDateTime createdAt,
        @Schema(description = "Timestamp when the course was last updated", example = "2026-10-31T12:30:00") LocalDateTime updatedAt) {
}
