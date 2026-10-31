package com.learnova.dto.user;


import com.learnova.model.Role;
import com.learnova.model.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}