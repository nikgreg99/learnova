package com.learnova.exception;

import lombok.Getter;

@Getter
public class QuizNotFoundException extends RuntimeException {

    private final Long quizId;

    public QuizNotFoundException(Long quizId) {
        super("Quiz not found with id: " + quizId);
        this.quizId = quizId;
    }
}
