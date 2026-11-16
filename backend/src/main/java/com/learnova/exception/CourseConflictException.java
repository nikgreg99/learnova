package com.learnova.exception;

public class CourseConflictException extends RuntimeException {

    public CourseConflictException(String message) {
        super(message);
    }
}
