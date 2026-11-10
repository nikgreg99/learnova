package com.learnova.exception;

import lombok.Getter;

@Getter
public class RatingNotFoundException extends RuntimeException {

    private final String message;

    public RatingNotFoundException(String message) {
        super(message);
        this.message = message;
    }
}
