package com.learnova.exception;

import lombok.Getter;

@Getter
public class UserProfileConflictException extends RuntimeException {

    private final String conflictingField;

    public UserProfileConflictException(String conflictingField) {
        super("The " + conflictingField + " is already in use.");
        this.conflictingField = conflictingField;
    }
}
