package com.learnova.exception;

import com.learnova.dto.ApiError;
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
