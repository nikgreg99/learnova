package com.learnova.service;

import com.learnova.client.KeycloakAdminClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock
    private KeycloakAdminClient keycloakClient;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    @Test
    void sendPasswordResetEmail_shouldDelegateToKeycloakClient() {
        // Arrange
        String email = "student@example.com";

        // Act
        keycloakUserService.sendPasswordResetEmail(email);

        // Assert
        verify(keycloakClient).sendPasswordResetEmail(email);
        verifyNoMoreInteractions(keycloakClient);
    }
}
