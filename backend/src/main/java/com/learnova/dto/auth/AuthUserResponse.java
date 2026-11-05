package com.learnova.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthUserResponse", description = "Information about the currently authenticated user")
public record AuthUserResponse(
        @Schema(description = "Unique identifier of the user", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") String id,
        @Schema(description = "Username of the authenticated user", example = "john.doe") String username,
        @Schema(description = "Email address of the authenticated user", example = "john.doe@example.com") String email,
        @Schema(description = "User's first name", example = "John") String firstName,
        @Schema(description = "User's last name", example = "Doe") String lastName) {
}
