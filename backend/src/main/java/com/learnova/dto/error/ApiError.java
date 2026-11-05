package com.learnova.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Standardized error response payload")
public record ApiError(
        @Schema(description = "UTC timestamp when the error occurred", example = "2026-10-31T12:00:00Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "404")
        int status,
        @Schema(description = "HTTP error reason phrase", example = "Not Found")
        String error,
        @Schema(description = "Detailed error message", example = "Course with ID 1 was not found")
        String message,
        @Schema(description = "Request URI path where the error occurred", example = "/courses/1")
        String path) {
}
