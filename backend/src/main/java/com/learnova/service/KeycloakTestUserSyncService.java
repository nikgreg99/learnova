package com.learnova.service;

import com.learnova.client.KeycloakAdminClient;
import com.learnova.client.KeycloakAdminClient.TestUser;
import com.learnova.model.Role;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import com.learnova.repository.RoleRepository;
import com.learnova.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KeycloakTestUserSyncService {

    private final KeycloakAdminClient keycloakAdminClient;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public KeycloakTestUserSyncService(KeycloakAdminClient keycloakAdminClient, UserRepository userRepository,
            RoleRepository roleRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public int syncMissingTestUsers() {
        int createdUsers = 0;
        for (TestUser keycloakUser : keycloakAdminClient.findTestUsers()) {
            if (userRepository.findByKeycloakId(keycloakUser.keycloakId()).isPresent()) {
                continue;
            }

            userRepository.findByEmail(keycloakUser.email()).ifPresent(existing -> {
                throw new IllegalStateException("Cannot synchronize Keycloak test user " + keycloakUser.username()
                        + ": email is already linked to another account");
            });

            userRepository.findByUsername(keycloakUser.username()).ifPresent(existing -> {
                throw new IllegalStateException("Cannot synchronize Keycloak test user " + keycloakUser.username()
                        + ": username is already linked to another account");
            });

            Set<Role> roles = new HashSet<>(roleRepository.findAllByNameIn(keycloakUser.realmRoles()));
            if (roles.isEmpty()) {
                roles.addAll(roleRepository.findAllByNameIn(Set.of("STUDENT")));
            }
            if (roles.isEmpty()) {
                throw new IllegalStateException("Cannot synchronize Keycloak test user " + keycloakUser.username()
                        + ": no application roles were found");
            }

            User user = User.builder().keycloakId(keycloakUser.keycloakId()).username(keycloakUser.username())
                    .email(keycloakUser.email()).firstName(keycloakUser.firstName()).lastName(keycloakUser.lastName())
                    .roles(roles).status(keycloakUser.enabled() ? UserStatus.ACTIVE : UserStatus.DISABLED).build();
            userRepository.save(user);
            createdUsers++;
        }
        return createdUsers;
    }
}
