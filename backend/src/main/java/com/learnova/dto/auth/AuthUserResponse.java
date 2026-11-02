package com.learnova.dto.auth;

public record AuthUserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName
) {}

