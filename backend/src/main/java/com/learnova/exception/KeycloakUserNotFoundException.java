package com.learnova.exception;

import lombok.Getter;

@Getter
public class KeycloakUserNotFoundException extends RuntimeException {

    private final String keycloakId;

    public KeycloakUserNotFoundException(String keycloakId) {
        super("Keycloak user with id " + keycloakId + " not found");
        this.keycloakId = keycloakId;
    }
}
