package com.learnova.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

@Schema(name = "UserRequest", description = "Request payload for creating a new Learnova user")
public record UserRequest(
        @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String firstName,
        @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String lastName,
        @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Unique username", example = "john.doe", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 100) String username,
        @Schema(description = "Roles assigned to the user", example = "[\"STUDENT\"]", requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty Set<@NotBlank String> roles,
        @Schema(description = "Initial user password", example = "StrongPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.WRITE_ONLY) @NotNull String password) {
}
