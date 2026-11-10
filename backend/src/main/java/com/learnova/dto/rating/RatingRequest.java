package com.learnova.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for submitting or updating a rating")
public record RatingRequest(
        @Schema(description = "Rating score from 1 to 5 stars", example = "5", minimum = "1", maximum = "5") @NotNull(message = "Rating value is required") @Min(value = 1, message = "Rating must be between 1 and 5") @Max(value = 5, message = "Rating must be between 1 and 5") Integer rating,

        @Schema(description = "Optional written feedback or review", example = "Great course, very comprehensive!") @Size(max = 2000, message = "Review cannot exceed 2000 characters") String review) {
}
