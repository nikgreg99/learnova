package com.learnova.dto.enrollment;

import com.learnova.model.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Response payload containing enrollment details")
public record EnrollmentResponse(
        @Schema(description = "Unique ID of the enrollment", example = "1")
        Long id,

        @Schema(description = "Unique ID of the enrolled student", example = "2")
        Long studentId,

        @Schema(description = "Username of the enrolled student", example = "mrossi")
        String studentUsername,

        @Schema(description = "Unique ID of the enrolled course", example = "5")
        Long courseId,

        @Schema(description = "Name of the enrolled course", example = "Spring Boot Essentials")
        String courseName,

        @Schema(description = "Current status of the enrollment", example = "ACTIVE")
        EnrollmentStatus status,

        @Schema(description = "Timestamp when the enrollment was created")
        LocalDateTime enrolledAt,

        @Schema(description = "Timestamp when the enrollment was last updated")
        LocalDateTime updatedAt) {}
