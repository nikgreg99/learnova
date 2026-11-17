package com.learnova.controller;

import com.learnova.dto.error.ApiError;
import com.learnova.dto.quiz.QuestionRequest;
import com.learnova.dto.quiz.QuestionResponse;
import com.learnova.dto.quiz.QuizPatchRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quizzes", description = "Quiz and Question authoring operations")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/quizzes/{quizId}")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "Get quiz details", description = "Retrieves full quiz details including questions and answers (Instructor view)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quiz retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasRole('ADMIN') or @quizSecurityService.isQuizOwner(#quizId)")
    @GetMapping
    public ResponseEntity<QuizResponse> getQuiz(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuizById(quizId));
    }

    @Operation(summary = "Update quiz", description = "Updates all fields of an existing quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quiz updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@quizSecurityService.isQuizOwner(#quizId)")
    @PutMapping
    public ResponseEntity<QuizResponse> updateQuiz(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.updateQuiz(quizId, request));
    }

    @Operation(summary = "Partially update quiz", description = "Updates selected fields of an existing quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quiz partially updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@quizSecurityService.isQuizOwner(#quizId)")
    @PatchMapping
    public ResponseEntity<QuizResponse> patchQuiz(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Valid @RequestBody QuizPatchRequest request) {
        return ResponseEntity.ok(quizService.patchQuiz(quizId, request));
    }

    @Operation(summary = "Delete quiz", description = "Deletes a quiz and all associated questions and attempts")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Quiz deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@quizSecurityService.isQuizOwner(#quizId)")
    @DeleteMapping
    public ResponseEntity<Void> deleteQuiz(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId) {
        quizService.deleteQuiz(quizId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add question to quiz", description = "Creates a new question with answer options within the specified quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Question created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid question format or options", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Question position conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@quizSecurityService.isQuizOwner(#quizId)")
    @PostMapping("/questions")
    public ResponseEntity<QuestionResponse> addQuestion(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Valid @RequestBody QuestionRequest request) {
        QuestionResponse response = quizService.addQuestion(quizId, request);
        URI location = URI.create("/api/v1/quizzes/" + quizId + "/questions/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update question in quiz", description = "Updates an existing question and its answer options within a quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid question format or options", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz or question not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Question position conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@quizSecurityService.isQuizOwner(#quizId)")
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Parameter(description = "Unique ID of the question", example = "1", required = true)
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(quizService.updateQuestion(quizId, questionId, request));
    }

    @Operation(summary = "Delete question from quiz", description = "Deletes a question and its answer options from the quiz")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Question deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not quiz owner", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Quiz or question not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("@quizSecurityService.isQuizOwner(#quizId)")
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @Parameter(description = "Unique ID of the quiz", example = "1", required = true)
            @PathVariable Long quizId,
            @Parameter(description = "Unique ID of the question", example = "1", required = true)
            @PathVariable Long questionId) {
        quizService.deleteQuestion(quizId, questionId);
        return ResponseEntity.noContent().build();
    }
}
