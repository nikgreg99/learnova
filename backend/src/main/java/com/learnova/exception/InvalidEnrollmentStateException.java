package com.learnova.exception;

import lombok.Getter;

@Getter
public class InvalidEnrollmentStateException extends RuntimeException {

    private final String reason;

    public InvalidEnrollmentStateException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
