package com.learnova.exception;

import com.learnova.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalMessageHandler {

    private final MessageSource messageSource;

    public GlobalMessageHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ApiError> handleCourseNotFoundException(
            CourseNotFoundException ex,
            HttpServletRequest request) {

        String message = this.messageSource.getMessage
                ("course.not-found",
                        new Object[] {ex.getCourseId()},
                        request.getLocale());

        return buildError(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleUserNotFoundException(
            UserNotFoundException ex,
            HttpServletRequest request
    ) {
        String message = messageSource.getMessage(
                "user.not-found",
                new Object[]{ex.getUserId()},
                request.getLocale()
        );

        return buildError(HttpStatus.NOT_FOUND, message, request);
    }

    @ExceptionHandler(UserDeletedException.class)
    public ResponseEntity<ApiError> handleUserDeletedException(
            UserDeletedException ex,
            HttpServletRequest request
    ) {
        String message = messageSource.getMessage(
                "user.deleted",
                new Object[]{ex.getUserId()},
                request.getLocale()
        );

        return buildError(HttpStatus.CONFLICT, message, request);
    }


    private ResponseEntity<ApiError> buildError(
            HttpStatus httpStatus,
            String message,
            HttpServletRequest request) {

        ApiError error = new ApiError(
                Instant.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(httpStatus.value()).body(error);
    }
}
