package com.learnova.exception;

import lombok.Getter;

@Getter
public class EnrollmentConflictException extends RuntimeException {

    private final String reason;

    public EnrollmentConflictException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
