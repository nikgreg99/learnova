package com.learnova.exception;

import lombok.Getter;

@Getter
public class QuizAttemptNotFoundException extends RuntimeException {

    private final Long attemptId;

    public QuizAttemptNotFoundException(Long attemptId) {
        super("Quiz attempt not found with id: " + attemptId);
        this.attemptId = attemptId;
    }
}
