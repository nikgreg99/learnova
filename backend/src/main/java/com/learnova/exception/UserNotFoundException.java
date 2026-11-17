package com.learnova.exception;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final Long userId;

    public UserNotFoundException(Long userId) {
        super("User with id " + userId + " was not found.");
        this.userId = userId;
    }
}
