package com.learnova.exception;

import lombok.Getter;

@Getter
public class UserDeletedException extends RuntimeException {

    private final Long userId;

    public UserDeletedException(Long userId) {
        super("User with id " + userId + " has been deleted and cannot be reactivated.");
        this.userId = userId;
    }
}
