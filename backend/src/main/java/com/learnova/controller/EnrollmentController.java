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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enrollments", description = "Operations for viewing and managing user enrollments")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "Get current student's enrollments", description = "Retrieves paginated enrollments for the authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollments retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<Page<EnrollmentResponse>> getMyEnrollments(
            @Parameter(description = "Optional status filter (ACTIVE, CANCELLED, COMPLETED)")
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(pageable, status));
    }

    @Operation(summary = "Get enrollment by ID", description = "Retrieves details of a specific enrollment. Accessible to the student, the course instructor, or an administrator.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment found"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not authorized to view this enrollment", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Enrollment not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> getEnrollmentById(
            @Parameter(description = "Unique ID of the enrollment", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(id));
    }

    @Operation(summary = "Cancel an enrollment", description = "Cancels an active enrollment. Restricted to the enrolled student and administrators.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid enrollment state (already cancelled or completed)", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not authorized to cancel this enrollment", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Enrollment not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<EnrollmentResponse> cancelEnrollment(
            @Parameter(description = "Unique ID of the enrollment", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.cancelEnrollment(id));
    }
}
