
package com.learnova.controller;

import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(
        name = "Authentication",
        description = "Authentication and current user operations"
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Initiate login",
            description = "Redirects the user to the Keycloak OAuth2 authorization flow."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "302",
                    description = "Redirect to Keycloak authorization"
            )
    })
    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/keycloak"))
                .build();
    }

    @Operation(
            summary = "Get current authenticated user",
            description = "Returns the profile of the currently authenticated user.",
            security = @SecurityRequirement(name = "oauth2")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authenticated user retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = AuthUserResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "User is not authenticated"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(oidcUser)
        );
    }
}
