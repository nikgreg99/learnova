package com.learnova.dto.rating;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Aggregated rating statistics for a course or lesson")
public record RatingSummaryResponse(
        @Schema(description = "Average rating score rounded to one decimal place, or null if no ratings exist", example = "4.8") Double averageRating,

        @Schema(description = "Total number of ratings submitted", example = "42") Long totalRatings) {
}
