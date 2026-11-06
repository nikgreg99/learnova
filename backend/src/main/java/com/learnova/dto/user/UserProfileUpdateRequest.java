package com.learnova.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserProfileUpdateRequest", description = "Request payload for updating a user's profile")
public record UserProfileUpdateRequest(
        @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String firstName,
        @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String lastName,
        @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Unique username", example = "john.doe", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String username) {
}
