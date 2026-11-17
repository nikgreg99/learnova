package com.learnova.exception;

import lombok.Getter;

@Getter
public class EnrollmentNotFoundException extends RuntimeException {

    private final Long enrollmentId;

    public EnrollmentNotFoundException(Long enrollmentId) {
        super("Enrollment not found with id: " + enrollmentId);
        this.enrollmentId = enrollmentId;
    }

    public EnrollmentNotFoundException(String message) {
        super(message);
        this.enrollmentId = null;
    }
}
