package com.learnova.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response payload for course publication readiness check")
public record CourseReadinessResponse(
        @Schema(description = "Whether the course is ready to be published", example = "true") boolean ready,
        @Schema(description = "List of reasons preventing publication if not ready", example = "[]") List<String> reasons) {
}
