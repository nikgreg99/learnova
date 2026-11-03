package com.learnova.controller;

import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/keycloak"))
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(
            @AuthenticationPrincipal OidcUser oidcUser
    ) {
        return ResponseEntity.ok(authService.getCurrentUser(oidcUser));
    }
}