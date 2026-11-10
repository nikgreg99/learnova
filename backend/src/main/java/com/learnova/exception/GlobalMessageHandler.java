package com.learnova.exception;

import com.learnova.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalMessageHandler {

    private final MessageSource messageSource;

    public GlobalMessageHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiError> handleCourseNotFoundException(CourseNotFoundException ex,
            HttpServletRequest request) {

        String message = this.messageSource.getMessage("course.not-found", new Object[]{
                ex.getCourseId()
        }, request.getLocale());

        return buildError(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        String message = messageSource.getMessage("user.not-found", new Object[]{
                ex.getUserId()
        }, request.getLocale());

        return buildError(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(UserDeletedException.class)
    public ResponseEntity<ApiError> handleUserDeletedException(UserDeletedException ex, HttpServletRequest request) {
        String message = messageSource.getMessage("user.deleted", new Object[]{
                ex.getUserId()
        }, request.getLocale());

        return buildError(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(UserProfileConflictException.class)
    public ResponseEntity<ApiError> handleUserProfileConflictException(UserProfileConflictException ex,
            HttpServletRequest request) {
        String message = messageSource.getMessage("user.profile-conflict", new Object[]{
                ex.getConflictingField()
        }, request.getLocale());

        return buildError(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(LessonNotFoundException.class)
    public ResponseEntity<ApiError> handleLessonNotFoundException(LessonNotFoundException ex,
            HttpServletRequest request) {
        String message = messageSource.getMessage("lesson.not-found", new Object[]{
                ex.getLessonId()
        }, request.getLocale());

        return buildError(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(LessonPositionConflictException.class)
    public ResponseEntity<ApiError> handleLessonPositionConflictException(LessonPositionConflictException ex,
            HttpServletRequest request) {
        String message = messageSource.getMessage("lesson.position-conflict", new Object[]{
                ex.getPosition()
        }, request.getLocale());

        return buildError(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(KeycloakUserNotFoundException.class)
    public ResponseEntity<ApiError> handleKeycloakUserNotFoundException(KeycloakUserNotFoundException ex,
            HttpServletRequest request) {
        String message = messageSource.getMessage("user.keycloak-not-found", new Object[]{
                ex.getKeycloakId()
        }, request.getLocale());

        return buildError(HttpStatus.CONFLICT, message, request);
    }

    @ExceptionHandler(RatingNotFoundException.class)
    public ResponseEntity<ApiError> handleRatingNotFoundException(RatingNotFoundException ex,
            HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateRatingException.class)
    public ResponseEntity<ApiError> handleDuplicateRatingException(DuplicateRatingException ex,
            HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> buildError(HttpStatus httpStatus, String message, HttpServletRequest request) {

        ApiError error = new ApiError(Instant.now(), httpStatus.value(), httpStatus.getReasonPhrase(), message,
                request.getRequestURI());

        return ResponseEntity.status(httpStatus.value()).body(error);
    }
}
