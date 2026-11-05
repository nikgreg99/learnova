package com.learnova.exception;

import lombok.Getter;

@Getter
public class UserDeletedException extends RuntimeException {

    private final Long userId;

    public UserDeletedException(Long userId) {
        this.userId = userId;
    }
}
