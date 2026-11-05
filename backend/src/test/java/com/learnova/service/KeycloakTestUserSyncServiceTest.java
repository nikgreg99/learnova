package com.learnova.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnova.client.KeycloakAdminClient;
import com.learnova.client.KeycloakAdminClient.TestUser;
import com.learnova.model.Role;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import com.learnova.repository.RoleRepository;
import com.learnova.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeycloakTestUserSyncServiceTest {

    private KeycloakAdminClient keycloakAdminClient;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private KeycloakTestUserSyncService syncService;

    @BeforeEach
    void setUp() {
        keycloakAdminClient = mock(KeycloakAdminClient.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        syncService = new KeycloakTestUserSyncService(keycloakAdminClient, userRepository, roleRepository);
    }

    @Test
    void syncMissingTestUsers_shouldCreateDatabaseProfile() {
        TestUser testUser = testUser(true);
        Role student = Role.builder().id(1L).name("STUDENT").build();
        when(keycloakAdminClient.findTestUsers()).thenReturn(List.of(testUser));
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test-student@learnova.local")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("test-student")).thenReturn(Optional.empty());
        when(roleRepository.findAllByNameIn(Set.of("STUDENT"))).thenReturn(List.of(student));

        int createdUsers = syncService.syncMissingTestUsers();

        assertThat(createdUsers).isEqualTo(1);
        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).satisfies(user -> {
            assertThat(user.getKeycloakId()).isEqualTo("keycloak-123");
            assertThat(user.getUsername()).isEqualTo("test-student");
            assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.getRoles()).containsExactly(student);
        });
    }

    @Test
    void syncMissingTestUsers_shouldSkipAlreadySynchronizedAccounts() {
        when(keycloakAdminClient.findTestUsers()).thenReturn(List.of(testUser(true)));
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.of(new User()));

        assertThat(syncService.syncMissingTestUsers()).isZero();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void syncMissingTestUsers_shouldFailWhenEmailBelongsToAnotherAccount() {
        when(keycloakAdminClient.findTestUsers()).thenReturn(List.of(testUser(true)));
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test-student@learnova.local")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(syncService::syncMissingTestUsers).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email is already linked");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void syncMissingTestUsers_shouldMarkDisabledKeycloakUsersDisabled() {
        TestUser testUser = testUser(false);
        Role student = Role.builder().id(1L).name("STUDENT").build();
        when(keycloakAdminClient.findTestUsers()).thenReturn(List.of(testUser));
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test-student@learnova.local")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("test-student")).thenReturn(Optional.empty());
        when(roleRepository.findAllByNameIn(Set.of("STUDENT"))).thenReturn(List.of(student));

        syncService.syncMissingTestUsers();

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void syncMissingTestUsers_shouldUseStudentRoleWhenNoApplicationRoleIsAssigned() {
        TestUser testUser = new TestUser("keycloak-123", "test-student", "test-student@learnova.local", "Test",
                "Student", true, Set.of("default-roles-learnova"));
        Role student = Role.builder().id(1L).name("STUDENT").build();
        when(keycloakAdminClient.findTestUsers()).thenReturn(List.of(testUser));
        when(userRepository.findByKeycloakId("keycloak-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test-student@learnova.local")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("test-student")).thenReturn(Optional.empty());
        when(roleRepository.findAllByNameIn(Set.of("default-roles-learnova"))).thenReturn(List.of());
        when(roleRepository.findAllByNameIn(Set.of("STUDENT"))).thenReturn(List.of(student));

        assertThat(syncService.syncMissingTestUsers()).isEqualTo(1);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(student);
    }

    private TestUser testUser(boolean enabled) {
        return new TestUser("keycloak-123", "test-student", "test-student@learnova.local", "Test", "Student", enabled,
                Set.of("STUDENT"));
    }
}
