package com.learnova.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnova.dto.auth.AuthUserResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.oauth2.jwt.Jwt;

class AuthMapperTest {

    private final AuthMapper authMapper =
        Mappers.getMapper(AuthMapper.class);

    @Test
    void toResponse_shouldMapJwtClaims() {
        Jwt jwt = createJwt(Map.of(
            "preferred_username", "test-user",
            "email", "test@example.com",
            "given_name", "John",
            "family_name", "Doe"
        ));

        AuthUserResponse result = authMapper.toResponse(jwt);

        assertThat(result.id()).isEqualTo("user-123");
        assertThat(result.username()).isEqualTo("test-user");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
    }

    @Test
    void toResponse_shouldMapNullWhenOptionalClaimsAreMissing() {
        Jwt jwt = createJwt(Map.of());

        AuthUserResponse result = authMapper.toResponse(jwt);

        assertThat(result.id()).isEqualTo("user-123");
        assertThat(result.username()).isNull();
        assertThat(result.email()).isNull();
        assertThat(result.firstName()).isNull();
        assertThat(result.lastName()).isNull();
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject("user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims(jwtClaims -> jwtClaims.putAll(claims))
            .build();
    }
}
