package com.learnova.controller;

import com.learnova.dto.error.ApiError;
import com.learnova.dto.lesson.LessonPatchRequest;
import com.learnova.dto.lesson.LessonRequest;
import com.learnova.dto.lesson.LessonResponse;
import com.learnova.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lessons", description = "Lesson management operations")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/courses/{courseId}/lessons")
@RestController
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Operation(summary = "Get all lessons of a course", description = "Retrieves an ordered list of all lessons for the specified course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of lessons retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<LessonResponse>> getLessons(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId) {
        return ResponseEntity.ok(lessonService.getLessons(courseId));
    }

    @Operation(summary = "Get lesson by ID", description = "Retrieves details of a specific lesson within a course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson found successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson or course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLesson(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Parameter(description = "Unique ID of the lesson", example = "1", required = true)
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getLessonById(courseId, lessonId));
    }

    @Operation(summary = "Create a lesson", description = "Creates a new lesson under the specified course")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lesson created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Lesson position conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Valid
            @RequestBody LessonRequest request) {
        LessonResponse created = lessonService.createLesson(courseId, request);
        URI location = URI.create(String.format("/api/v1/courses/%d/lessons/%d", courseId, created.id()));
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Update an existing lesson", description = "Updates details of an existing lesson within a course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lesson or course not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Lesson position conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> updateLesson(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Parameter(description = "Unique ID of the lesson to update", example = "1", required = true)
            @PathVariable Long lessonId,
            @Valid
            @RequestBody LessonRequest request) {
        return ResponseEntity.ok(lessonService.updateLesson(courseId, lessonId, request));
    }

    @Operation(summary = "Partially update an existing lesson", description = "Updates selected fields of an existing lesson within a course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson partially updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lesson or course not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Lesson position conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> patchLesson(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Parameter(description = "Unique ID of the lesson to update", example = "1", required = true)
            @PathVariable Long lessonId,
            @Valid
            @RequestBody LessonPatchRequest request) {
        return ResponseEntity.ok(lessonService.patchLesson(courseId, lessonId, request));
    }

    @Operation(summary = "Delete a lesson", description = "Deletes a lesson from the specified course")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Lesson deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson or course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Parameter(description = "Unique ID of the lesson to delete", example = "1", required = true)
            @PathVariable Long lessonId) {
        lessonService.deleteLesson(courseId, lessonId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
