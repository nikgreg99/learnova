package com.learnova.exception;

import lombok.Getter;

@Getter
public class RatingNotFoundException extends RuntimeException {

    private final String reason;

    public RatingNotFoundException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
