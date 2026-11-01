package com.learnova.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating or updating a course")
public record CourseRequest(
                @Schema(description = "Name of the course", example = "Spring masterclass") @NotBlank String name,
                @Schema(description = "Description of the course", example = "Comprehensive guide to master Spring boot") String description,
                @Schema(description = "Price of the course", example = "49.99") @PositiveOrZero @NotNull BigDecimal price) {
}
