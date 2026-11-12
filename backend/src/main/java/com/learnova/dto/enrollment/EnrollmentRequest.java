package com.learnova.dto.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload for enrolling in a course")
public record EnrollmentRequest(
        @Schema(description = "ID of the course to enroll in", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Course ID must not be null")
        Long courseId) {}
