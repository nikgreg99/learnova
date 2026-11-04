
package com.learnova.controller;

import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private AuthUserResponse authUserResponse;

    @BeforeEach
    void setUp() {
        authUserResponse = new AuthUserResponse(
                "123456",
                "test-student",
                "test-student@learnova.local",
                "Test",
                "Student"
        );
    }

    private Jwt createJwt() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-access-token")
                .header("alg", "RS256")
                .subject("123456")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("preferred_username", "test-student")
                .claim("email", "test-student@learnova.local")
                .claim("given_name", "Test")
                .claim("family_name", "Student")
                .build();
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedUser() throws Exception {

        when(authService.getCurrentUser(any(Jwt.class)))
                .thenReturn(authUserResponse);

        mockMvc.perform(get("/api/auth/me")
                        .with(jwt().jwt(createJwt())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("123456"))
                .andExpect(jsonPath("$.username").value("test-student"))
                .andExpect(jsonPath("$.email")
                        .value("test-student@learnova.local"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Student"));
    }

    @Test
    void getCurrentUser_shouldReturnUnauthorizedWhenUnauthenticated()
            throws Exception {

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
