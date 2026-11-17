package com.learnova.controller;

import com.learnova.dto.error.ApiError;
import com.learnova.dto.quiz.QuizAttemptResponse;
import com.learnova.dto.quiz.QuizAttemptSubmitRequest;
import com.learnova.dto.quiz.QuizTakeResponse;
import com.learnova.service.QuizAttemptService;
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

@Tag(name = "Quiz Attempts", description = "Student quiz taking and attempt operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/quizzes/{quizId}")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService quizAttemptService;

    @Operation(summary = "Get quiz for taking", description = "Retrieves quiz questions and options without answer keys (Zero-leak student view)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quiz retrieved successfully for taking"),
            @ApiResponse(responseCode = "400", description = "Quiz inactive or course unpublished", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not actively enrolled", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/take")
    public ResponseEntity<QuizTakeResponse> getQuizForTaking(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId) {
        return ResponseEntity.ok(quizAttemptService.getQuizForTaking(quizId));
    }

    @Operation(summary = "Start a quiz attempt", description = "Begins a new attempt for the specified quiz, validating enrollment and attempt limits")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attempt started successfully"),
            @ApiResponse(responseCode = "400", description = "Quiz inactive, unpublished, or max attempts reached", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not actively enrolled", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Attempt already in progress", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/attempts")
    public ResponseEntity<QuizAttemptResponse> startAttempt(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId) {
        QuizAttemptResponse response = quizAttemptService.startAttempt(quizId);
        URI location = URI.create("/api/v1/quizzes/" + quizId + "/attempts/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Submit quiz attempt answers", description = "Submits answers for an in-progress attempt, triggers auto-grading, and calculates score")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attempt submitted and graded successfully"),
            @ApiResponse(responseCode = "400", description = "Attempt already submitted or invalid answers", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not owner of attempt", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz or attempt not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<QuizAttemptResponse> submitAttempt(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Parameter(description = "Unique ID of the attempt", example = "1", required = true)
            @PathVariable Long attemptId,
            @Valid @RequestBody(required = false) QuizAttemptSubmitRequest request) {
        return ResponseEntity.ok(quizAttemptService.submitAttempt(quizId, attemptId, request));
    }

    @Operation(summary = "Get student's quiz attempts", description = "Retrieves all attempts made by the authenticated student for this quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attempts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/attempts")
    public ResponseEntity<List<QuizAttemptResponse>> getStudentAttempts(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId) {
        return ResponseEntity.ok(quizAttemptService.getStudentAttempts(quizId));
    }

    @Operation(summary = "Get quiz attempt details", description = "Retrieves details and grading results of a specific quiz attempt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attempt retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not owner of attempt or course instructor", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz or attempt not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<QuizAttemptResponse> getAttempt(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Parameter(description = "Unique ID of the attempt", example = "1", required = true)
            @PathVariable Long attemptId) {
        return ResponseEntity.ok(quizAttemptService.getAttemptById(quizId, attemptId));
    }

    @Operation(summary = "Get all attempts for a quiz (Instructor view)", description = "Retrieves all student attempts for the quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attempts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not course owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasRole('ADMIN') or @quizSecurityService.isQuizOwner(#quizId)")
    @GetMapping("/attempts/all")
    public ResponseEntity<List<QuizAttemptResponse>> getAllAttempts(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId) {
        return ResponseEntity.ok(quizAttemptService.getAllAttemptsForQuiz(quizId));
    }
}
