package com.learnova.controller;

import com.learnova.dto.error.ApiError;
import com.learnova.dto.rating.RatingRequest;
import com.learnova.dto.rating.RatingResponse;
import com.learnova.dto.rating.RatingSummaryResponse;
import com.learnova.service.RatingService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lesson Ratings", description = "Operations for rating and reviewing lessons")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/lessons/{lessonId}/ratings")
public class LessonRatingController {

    private final RatingService ratingService;

    public LessonRatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Operation(summary = "Submit a rating for a lesson", description = "Submits a rating and optional review for the specified lesson")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rating submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Lesson not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "User has already rated this lesson", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @Parameter(description = "Unique ID of the lesson", example = "10", required = true)
            @PathVariable Long lessonId,
            @Valid
            @RequestBody RatingRequest request) {
        RatingResponse response = ratingService.createLessonRating(lessonId, request);
        URI location = URI.create(String.format("/api/v1/lessons/%d/ratings/%d", lessonId, response.id()));
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get all ratings for a lesson", description = "Retrieves all submitted ratings and reviews for a lesson")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of ratings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<RatingResponse>> getRatings(
            @Parameter(description = "Unique ID of the lesson", example = "10", required = true)
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(ratingService.getLessonRatings(lessonId));
    }

    @Operation(summary = "Get rating summary for a lesson", description = "Retrieves the average rating score and total count of ratings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Lesson not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<RatingSummaryResponse> getRatingSummary(
            @Parameter(description = "Unique ID of the lesson", example = "10", required = true)
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(ratingService.getLessonRatingSummary(lessonId));
    }

    @Operation(summary = "Get current user's rating for a lesson", description = "Retrieves the authenticated user's own rating for the specified lesson")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User rating found"),
            @ApiResponse(responseCode = "404", description = "Rating or lesson not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @GetMapping("/me")
    public ResponseEntity<RatingResponse> getMyRating(
            @Parameter(description = "Unique ID of the lesson", example = "10", required = true)
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(ratingService.getMyLessonRating(lessonId));
    }

    @Operation(summary = "Update current user's lesson rating", description = "Updates the authenticated user's rating score and review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Rating or lesson not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @PutMapping("/me")
    public ResponseEntity<RatingResponse> updateMyRating(
            @Parameter(description = "Unique ID of the lesson", example = "10", required = true)
            @PathVariable Long lessonId,
            @Valid
            @RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.updateMyLessonRating(lessonId, request));
    }

    @Operation(summary = "Delete current user's lesson rating", description = "Deletes the authenticated user's rating for the specified lesson")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rating deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rating or lesson not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PreAuthorize("hasAnyRole('STUDENT', 'INSTRUCTOR', 'ADMIN')")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyRating(
            @Parameter(description = "Unique ID of the lesson", example = "10", required = true)
            @PathVariable Long lessonId) {
        ratingService.deleteMyLessonRating(lessonId);
        return ResponseEntity.noContent().build();
    }
}
