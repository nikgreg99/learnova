package com.learnova.exception;

import lombok.Getter;

@Getter
public class LessonPositionConflictException extends RuntimeException {

    private final Integer position;

    public LessonPositionConflictException(Integer position) {
        this.position = position;
    }
}
