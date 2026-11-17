package com.learnova.exception;

import lombok.Getter;

@Getter
public class InvalidQuizStateException extends RuntimeException {

    private final String reason;

    public InvalidQuizStateException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
