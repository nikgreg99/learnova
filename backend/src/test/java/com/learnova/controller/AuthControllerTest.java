
package com.learnova.controller;

import com.learnova.config.TestOAuth2Config;
import com.learnova.config.security.SecurityConfig;
import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import static org.hamcrest.Matchers.containsString;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        TestOAuth2Config.class
})
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

    private ClientRegistration keycloakRegistration() {
        return ClientRegistration.withRegistrationId("keycloak")
                .clientId("learnova-backend")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                )
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE
                )
                .redirectUri(
                        "{baseUrl}/login/oauth2/code/{registrationId}"
                )
                .scope("openid", "profile", "email")
                .authorizationUri("http://localhost:9999/authorize")
                .tokenUri("http://localhost:9999/token")
                .userInfoUri("http://localhost:9999/userinfo")
                .userNameAttributeName("sub")
                .jwkSetUri("http://localhost:9999/certs")
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint",
                        "http://localhost:9999/logout"
                ))
                .build();
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedUser() throws Exception {
        when(authService.getCurrentUser(any(OidcUser.class)))
                .thenReturn(authUserResponse);

        mockMvc.perform(get("/api/auth/me")
                        .with(oidcLogin()
                                .clientRegistration(keycloakRegistration())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value("123456"))
                .andExpect(jsonPath("$.username").value("test-student"))
                .andExpect(jsonPath("$.email")
                        .value("test-student@learnova.local"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Student"));
    }

    @Test
    void getCurrentUser_shouldRedirectWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void logout_shouldReturnForbiddenWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(oidcLogin()
                                .clientRegistration(keycloakRegistration())))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_shouldRedirectToKeycloakWhenCsrfTokenIsValid()
            throws Exception {

        mockMvc.perform(post("/api/auth/logout")
                        .with(oidcLogin()
                                .clientRegistration(keycloakRegistration()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        containsString("http://localhost:9999/logout")
                ));
    }
}
