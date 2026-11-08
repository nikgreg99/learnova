package com.learnova.config.keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken>
{
    private static final String REALM_ACCESS = "realm_access";
    private static final String ROLES = "roles";

    private static final Set<String> SUPPORTED_ROLES = Set.of(
        "STUDENT",
        "INSTRUCTOR",
        "ADMIN"
    );

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Map<String, Object> realmAccess =
            jwt.getClaimAsMap(REALM_ACCESS);

        if (realmAccess == null) {
            return new JwtAuthenticationToken(jwt, List.of());
        }

        Object rolesClaim = realmAccess.get(ROLES);

        if (!(rolesClaim instanceof Collection<?> roles)) {
            return new JwtAuthenticationToken(jwt, List.of());
        }

        var authorities = roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(SUPPORTED_ROLES::contains)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .toList();

        return new JwtAuthenticationToken(
            jwt,
            authorities,
            jwt.getSubject()
        );
    }
}
