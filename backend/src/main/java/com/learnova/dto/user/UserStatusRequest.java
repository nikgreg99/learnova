package com.learnova.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "UserStatusRequest", description = "Request payload for changing an account's status")
public record UserStatusRequest(
        @Schema(description = "The target account status", example = "DISABLED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Status status) {
    public enum Status {
        ACTIVE, DISABLED
    }
}
