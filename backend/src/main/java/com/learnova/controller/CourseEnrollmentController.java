package com.learnova.controller;

import com.learnova.dto.enrollment.EnrollmentResponse;
import com.learnova.dto.error.ApiError;
import com.learnova.model.EnrollmentStatus;
import com.learnova.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course Enrollments", description = "Operations for enrolling in courses and managing course rosters")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/courses/{courseId}/enrollments")
public class CourseEnrollmentController {

    private final EnrollmentService enrollmentService;

    public CourseEnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "Enroll in a course", description = "Enrolls the authenticated student into the specified course")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Enrolled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Student is already enrolled in this course", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId) {
        EnrollmentResponse response = enrollmentService.enrollStudent(courseId);
        URI location = URI.create(String.format("/api/v1/enrollments/%d", response.id()));
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get course enrollments roster", description = "Retrieves paginated enrollments for a course. Restricted to the course instructor and administrators.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment roster retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not the course owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<EnrollmentResponse>> getCourseEnrollments(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Parameter(description = "Optional status filter (ACTIVE, CANCELLED, COMPLETED)")
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId, pageable, status));
    }
}
