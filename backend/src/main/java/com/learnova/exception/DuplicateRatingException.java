package com.learnova.exception;

import lombok.Getter;

@Getter
public class DuplicateRatingException extends RuntimeException {

    private final String message;

    public DuplicateRatingException(String message) {
        super(message);
        this.message = message;
    }
}
