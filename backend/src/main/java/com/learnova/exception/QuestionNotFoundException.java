package com.learnova.exception;

import lombok.Getter;

@Getter
public class QuestionNotFoundException extends RuntimeException {

    private final Long questionId;

    public QuestionNotFoundException(Long questionId) {
        super("Question not found with id: " + questionId);
        this.questionId = questionId;
    }
}
