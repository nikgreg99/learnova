package com.learnova.controller;

import com.learnova.dto.course.CoursePatchRequest;
import com.learnova.dto.course.CourseRequest;
import com.learnova.dto.course.CourseResponse;
import com.learnova.dto.course.CourseStatusUpdateRequest;
import com.learnova.dto.error.ApiError;
import com.learnova.service.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Courses", description = "Course management operations")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/courses")
@RestController
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(
        summary = "Get all courses",
        description = "Retrieves a list of all available courses in the catalog"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of courses retrieved successfully"
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<CourseResponse>> getCourses() {
        return ResponseEntity.ok(courseService.getCourses());
    }

    @Operation(
        summary = "Get course by ID",
        description = "Retrieves details of a specific course by its unique identifier"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Course found successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> getCourse(
        @Parameter(
            description = "Unique ID of the course",
            example = "1",
            required = true
        )
        @PathVariable Long id
    ) {
        return courseService.getCourseById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Create a new course",
        description = "Creates a new course with name, description, and price"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Course created successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or validation failure",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<CourseResponse> addCourse(
        @RequestBody @Valid CourseRequest course
    ) {
        var createdCourse = courseService.addCourse(course);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdCourse);
    }

    @Operation(
        summary = "Update an existing course",
        description = "Updates details of an existing course by its unique identifier"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Course updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or validation failure",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to update this course",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize(
        "hasRole('ADMIN') or " +
            "(hasRole('INSTRUCTOR') and @courseSecurityService.isOwner(#id))"
    )
    public ResponseEntity<CourseResponse> updateCourse(
        @Parameter(
            description = "Unique ID of the course to update",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @RequestBody @Valid CourseRequest courseRequest
    ) {
        var course = courseService.updateCourse(id, courseRequest);

        return course.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Partially update an existing course",
        description = "Updates selected fields of an existing course"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Course partially updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or validation failure",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to update this course",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PatchMapping("/{id}")
    @PreAuthorize(
        "hasRole('ADMIN') or " +
            "(hasRole('INSTRUCTOR') and @courseSecurityService.isOwner(#id))"
    )
    public ResponseEntity<CourseResponse> patchCourse(
        @Parameter(
            description = "Unique ID of the course to update",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @RequestBody @Valid CoursePatchRequest patchRequest
    ) {
        var course = courseService.patchCourse(id, patchRequest);

        return course.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Update course publication status",
        description = "Updates the publication status of an existing course"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Course status updated successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or validation failure",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to update this course status",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize(
        "hasRole('ADMIN') or " +
            "(hasRole('INSTRUCTOR') and @courseSecurityService.isOwner(#id))"
    )
    public ResponseEntity<CourseResponse> updateCourseStatus(
        @Parameter(
            description = "Unique ID of the course to update",
            example = "1",
            required = true
        )
        @PathVariable Long id,
        @RequestBody @Valid CourseStatusUpdateRequest statusRequest
    ) {
        var course = courseService.updateCourseStatus(
            id,
            statusRequest.status()
        );

        return course.map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Delete a course by ID",
        description = "Deletes a course from the catalog by its unique identifier"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Course deleted successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to delete this course",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Course not found",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(
        "hasRole('ADMIN') or " +
            "(hasRole('INSTRUCTOR') and @courseSecurityService.isOwner(#id))"
    )
    public ResponseEntity<Void> deleteCourse(
        @Parameter(
            description = "Unique ID of the course to delete",
            example = "1",
            required = true
        )
        @PathVariable Long id
    ) {
        courseService.deleteCourseById(id);

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }
}

