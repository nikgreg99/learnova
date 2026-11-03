package com.learnova.dto.user;

import com.learnova.model.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String username,
        Set<String> roles,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}