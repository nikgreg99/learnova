package com.learnova.exception;

import lombok.Getter;

@Getter
public class LessonPositionConflictException extends RuntimeException {

    private final Integer position;

    public LessonPositionConflictException(Integer position) {
        super("A lesson with position " + position + " already exists in this course.");
        this.position = position;
    }
}
