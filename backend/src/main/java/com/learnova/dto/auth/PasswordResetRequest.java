package com.learnova.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to initiate a password reset")
public record PasswordResetRequest(

    @NotBlank
    @Email
    @Size(max = 254)
    @Schema(description = "Email address associated with the account",
        example = "student@example.com")
    String email

) {
}
