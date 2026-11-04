package com.learnova.service;

import com.learnova.client.KeycloakAdminClient;
import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserProfileUpdateRequest;
import org.springframework.stereotype.Service;

@Service
public class KeycloakUserService {

    private final KeycloakAdminClient keycloakClient;

    public KeycloakUserService(KeycloakAdminClient keycloakUserService) {
        this.keycloakClient = keycloakUserService;
    }

    public String createUser(UserRequest request){
        return keycloakClient.createUser(request);
    }

    public void setEnabled(String keycloakUserId, boolean enabled) {
        keycloakClient.setUserEnabled(keycloakUserId, enabled);
    }

    public void updateProfile(
            String keycloakUserId,
            UserProfileUpdateRequest request
    ) {
        keycloakClient.updateUserProfile(keycloakUserId, request);
    }

}
