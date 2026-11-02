package com.learnova.config;

import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@TestConfiguration
public class TestOAuth2Config {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {

        ClientRegistration registration = ClientRegistration
                .withRegistrationId("keycloak")
                .clientId("learnova-backend")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
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

        return new InMemoryClientRegistrationRepository(registration);
    }
}