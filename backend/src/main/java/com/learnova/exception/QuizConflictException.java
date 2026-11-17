package com.learnova.exception;

import lombok.Getter;

@Getter
public class QuizConflictException extends RuntimeException {

    private final String reason;

    public QuizConflictException(String reason) {
        super(reason);
        this.reason = reason;
    }
}
