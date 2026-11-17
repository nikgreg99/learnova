package com.learnova.exception;

import lombok.Getter;

@Getter
public class InvalidCourseStateException extends RuntimeException {

    private final String reason;

    public InvalidCourseStateException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
