package com.learnova.controller;

import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.dto.auth.PasswordResetRequest;
import com.learnova.service.AuthService;
import com.learnova.service.KeycloakUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Authentication", description = "Authentication and current user operations")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final KeycloakUserService keycloakUserService;

    public AuthController(AuthService authService, KeycloakUserService keycloakUserService) {
        this.authService = authService;
        this.keycloakUserService = keycloakUserService;
    }

    @Operation(summary = "Get current authenticated user", description = "Returns the profile of the currently authenticated user.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated user retrieved successfully", content = @Content(schema = @Schema(implementation = AuthUserResponse.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(@Parameter(hidden = true)
    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getCurrentUser(jwt));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(
        @Valid @RequestBody PasswordResetRequest request) {

        try {
            keycloakUserService.sendPasswordResetEmail(request.email());
        } catch (Exception ex) {
            log.warn("Unable to process password reset request", ex);
        }

        return ResponseEntity.accepted().build();
    }

}
