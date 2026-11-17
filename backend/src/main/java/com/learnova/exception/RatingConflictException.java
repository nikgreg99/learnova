package com.learnova.exception;

import lombok.Getter;

@Getter
public class RatingConflictException extends RuntimeException {

    private final String reason;

    public RatingConflictException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
