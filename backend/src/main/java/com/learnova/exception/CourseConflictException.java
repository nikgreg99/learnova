package com.learnova.exception;

import lombok.Getter;

@Getter
public class CourseConflictException extends RuntimeException {

    private final String reason;

    public CourseConflictException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
