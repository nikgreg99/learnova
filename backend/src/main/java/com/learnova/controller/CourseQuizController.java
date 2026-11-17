package com.learnova.controller;

import com.learnova.dto.error.ApiError;
import com.learnova.dto.quiz.QuizRequest;
import com.learnova.dto.quiz.QuizResponse;
import com.learnova.service.QuizService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course Quizzes", description = "Course quiz management operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/courses/{courseId}/quizzes")
@RequiredArgsConstructor
public class CourseQuizController {

    private final QuizService quizService;

    @Operation(summary = "Create a quiz for a course", description = "Creates a new quiz within the specified course")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quiz created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not course owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@courseSecurityService.isOwner(#courseId)")
    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId,
            @Valid @RequestBody QuizRequest request) {
        QuizResponse response = quizService.createQuiz(courseId, request);
        URI location = URI.create("/api/v1/quizzes/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get all quizzes for a course", description = "Retrieves quizzes for the course. Students only see active quizzes; instructors/admins see all.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quizzes retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasRole('ADMIN') or @courseSecurityService.canView(#courseId)")
    @GetMapping
    public ResponseEntity<List<QuizResponse>> getQuizzes(
            @Parameter(description = "Unique ID of the course", example = "1", required = true)
            @PathVariable Long courseId) {
        return ResponseEntity.ok(quizService.getCourseQuizzes(courseId));
    }
}
