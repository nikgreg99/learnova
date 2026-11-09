package com.learnova.client;


import com.learnova.config.keycloak.KeycloakJwtAuthenticationConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KeycloakJwtAuthenticationConverter();
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("test-user-id")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims(jwtClaims -> jwtClaims.putAll(claims))
            .build();
    }

    @Test
    void shouldConvertStudentRole() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", List.of("STUDENT"))
        ));

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertEquals(
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")),
            authentication.getAuthorities().stream().toList()
        );
    }

    @Test
    void shouldConvertInstructorRole() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", List.of("INSTRUCTOR"))
        ));

        var authentication = converter.convert(jwt);

        assertTrue(authentication.getAuthorities()
            .contains(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")));
    }

    @Test
    void shouldConvertAdminRole() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", List.of("ADMIN"))
        ));

        var authentication = converter.convert(jwt);

        assertTrue(authentication.getAuthorities()
            .contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void shouldConvertMultipleRoles() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", List.of("STUDENT", "INSTRUCTOR", "ADMIN"))
        ));

        var authentication = converter.convert(jwt);

        assertEquals(3, authentication.getAuthorities().size());
    }

    @Test
    void shouldIgnoreUnsupportedRoles() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", List.of("STUDENT", "SUPER_ADMIN"))
        ));

        var authentication = converter.convert(jwt);

        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities()
            .contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRealmAccessIsMissing() {
        Jwt jwt = createJwt(Map.of());

        var authentication = converter.convert(jwt);

        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().isEmpty());
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenRolesAreMissing() {
        Jwt jwt = createJwt(Map.of(
            "realm_access", Map.of()
        ));

        var authentication = converter.convert(jwt);

        assertTrue(authentication.getAuthorities().isEmpty());
    }

    @Test
    void shouldPreserveJwtSubjectAsPrincipalName() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", List.of("STUDENT"))
        ));

        var authentication = converter.convert(jwt);

        assertEquals("test-user-id", authentication.getName());
    }

    @Test
    void shouldIgnoreNonStringRoles() {
        Jwt jwt = createJwt(Map.of(
            "realm_access",
            Map.of("roles", Arrays.asList("STUDENT", 123, null))
        ));

        var authentication = converter.convert(jwt);

        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities()
            .contains(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }
}
