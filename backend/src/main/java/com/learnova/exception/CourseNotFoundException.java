package com.learnova.exception;

import lombok.Getter;

@Getter
public class CourseNotFoundException extends RuntimeException {
    private final Long courseId;

    public CourseNotFoundException(Long courseId) {
        this.courseId = courseId;
    }

}
