package com.learnova.exception;

public class InvalidCourseStateException extends RuntimeException {

    public InvalidCourseStateException(String message) {
        super(message);
    }
}
