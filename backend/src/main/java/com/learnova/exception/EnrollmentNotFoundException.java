package com.learnova.exception;

public class EnrollmentNotFoundException extends RuntimeException {

    public EnrollmentNotFoundException(String message) {
        super(message);
    }

    public EnrollmentNotFoundException(Long id) {
        super("Enrollment not found with id: " + id);
    }
}
