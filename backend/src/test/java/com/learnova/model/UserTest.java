package com.learnova.model;



import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void user_ShouldCreateUserUsingNoArgsConstructor() {
        User user = new User();

        assertNull(user.getId());
        assertNull(user.getKeycloakId());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getEmail());
        assertNull(user.getUsername());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());
        assertNull(user.getStatus());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    void user_ShouldCreateUserUsingAllArgsConstructor() {
        Role role = Role.builder()
            .id(1L)
            .name("STUDENT")
            .description("Student role")
            .build();

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        User user = new User(
            1L,
            "keycloak-123",
            "John",
            "Doe",
            "john.doe@example.com",
            "johndoe",
            roles,
            UserStatus.ACTIVE,
            createdAt,
            updatedAt
        );

        assertEquals(1L, user.getId());
        assertEquals("keycloak-123", user.getKeycloakId());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("johndoe", user.getUsername());
        assertEquals(roles, user.getRoles());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
    }

    @Test
    void user_ShouldCreateUserUsingBuilder() {
        User user = User.builder()
            .id(1L)
            .keycloakId("keycloak-123")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .username("johndoe")
            .status(UserStatus.ACTIVE)
            .build();

        assertEquals(1L, user.getId());
        assertEquals("keycloak-123", user.getKeycloakId());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("johndoe", user.getUsername());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    void user_ShouldUpdateUserFields() {
        User user = new User();

        Role role = Role.builder()
            .id(1L)
            .name("ADMIN")
            .build();

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();

        user.setId(1L);
        user.setKeycloakId("keycloak-456");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane.smith@example.com");
        user.setUsername("janesmith");
        user.setRoles(Set.of(role));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        assertEquals(1L, user.getId());
        assertEquals("keycloak-456", user.getKeycloakId());
        assertEquals("Jane", user.getFirstName());
        assertEquals("Smith", user.getLastName());
        assertEquals("jane.smith@example.com", user.getEmail());
        assertEquals("janesmith", user.getUsername());
        assertEquals(Set.of(role), user.getRoles());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
    }

    @Test
    void user_ShouldAllowNullUsername() {
        User user = User.builder()
            .keycloakId("keycloak-123")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .status(UserStatus.ACTIVE)
            .build();

        assertNull(user.getUsername());
    }

    @Test
    void usr_ShouldInitializeRolesWhenUsingBuilderWithoutRoles() {
        User user = User.builder()
            .keycloakId("keycloak-123")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .status(UserStatus.ACTIVE)
            .build();

        assertNotNull(user.getRoles());
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    void user_ShouldAddRoleToUser() {
        User user = User.builder()
            .roles(new HashSet<>())
            .build();

        Role role = Role.builder()
            .id(1L)
            .name("INSTRUCTOR")
            .build();

        user.getRoles().add(role);

        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(role));
    }

    @Test
    void user_ShouldRemoveRoleFromUser() {
        Role role = Role.builder()
            .id(1L)
            .name("STUDENT")
            .build();

        User user = User.builder()
            .roles(new HashSet<>(Set.of(role)))
            .build();

        user.getRoles().remove(role);

        assertTrue(user.getRoles().isEmpty());
    }
}
