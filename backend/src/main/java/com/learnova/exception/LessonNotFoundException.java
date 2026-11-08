package com.learnova.exception;

import lombok.Getter;

@Getter
public class LessonNotFoundException extends RuntimeException {

    private final Long lessonId;

    public LessonNotFoundException(Long lessonId) {
        this.lessonId = lessonId;
    }
}
