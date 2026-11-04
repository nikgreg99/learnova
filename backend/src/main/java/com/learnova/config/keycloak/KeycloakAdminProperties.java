package com.learnova.config.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakAdminProperties(
        String scheme,
        String host,
        int port,
        String realm,
        Admin admin
) {
    public String baseUrl() {
        return scheme + "://" + host + ":" + port;
    }

    public record Admin(
            String clientId,
            String clientSecret
    ) {}
}
