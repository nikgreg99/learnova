
package com.learnova.service;

import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.exception.UserDeletedException;
import com.learnova.exception.UserNotFoundException;
import com.learnova.mapper.UserMapper;
import com.learnova.model.Role;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import com.learnova.repository.RoleRepository;
import com.learnova.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private UserRequest request;
    private User user;
    private Role studentRole;
    private Role instructorRole;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        request = new UserRequest(
                "Mario",
                "Rossi",
                "mario.rossi@example.com",
                "mrossi",
                Set.of("STUDENT", "INSTRUCTOR"),
                "SecurePassword123!"
        );

        user = new User();
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");

        studentRole = Role.builder()
                .id(1L)
                .name("STUDENT")
                .description("Student")
                .build();

        instructorRole = Role.builder()
                .id(2L)
                .name("INSTRUCTOR")
                .description("Instructor")
                .build();

        response = new UserResponse(
                1L,
                "Mario",
                "Rossi",
                "mario.rossi@example.com",
                "mrossi",
                Set.of("STUDENT", "INSTRUCTOR"),
                UserStatus.ACTIVE,
                null,
                null
        );
    }

    // CREATE USER

    @Test
    void createUser_shouldCreateUserWithMultipleRoles() {
        when(keycloakUserService.createUser(request))
                .thenReturn("keycloak-123");

        when(userMapper.toEntity(request))
                .thenReturn(user);

        when(roleRepository.findAllByNameIn(request.roles()))
                .thenReturn(List.of(studentRole, instructorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);
                    savedUser.setId(1L);
                    return savedUser;
                });

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals(Set.of("STUDENT", "INSTRUCTOR"), result.roles());

        assertEquals("keycloak-123", user.getKeycloakId());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(Set.of(studentRole, instructorRole), user.getRoles());

        verify(keycloakUserService).createUser(request);
        verify(roleRepository).findAllByNameIn(request.roles());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void createUser_shouldThrowExceptionWhenRoleDoesNotExist() {
        when(keycloakUserService.createUser(request))
                .thenReturn("keycloak-123");

        when(userMapper.toEntity(request))
                .thenReturn(user);

        when(roleRepository.findAllByNameIn(request.roles()))
                .thenReturn(List.of(studentRole));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    // DISABLE USER

    @Test
    void disableUser_shouldSetStatusToDisabled() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result = userService.disableUser(id);

        assertEquals(UserStatus.DISABLED, user.getStatus());
        assertNotNull(result);

        verify(keycloakUserService).setEnabled(user.getKeycloakId(), false);
        verify(userRepository).findById(id);
        verify(userMapper).toResponse(user);
    }

    @Test
    void disableUser_shouldThrowWhenUserNotFound() {
        Long id = 99L;

        when(userRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.disableUser(id)
        );

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void disableUser_shouldRejectDeletedUserWithoutChangingKeycloak() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        assertThrows(
                UserDeletedException.class,
                () -> userService.disableUser(id)
        );

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void disableUser_shouldKeepLocalStatusWhenKeycloakUpdateFails() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Keycloak update failed"))
                .when(keycloakUserService)
                .setEnabled(user.getKeycloakId(), false);

        assertThrows(
                IllegalStateException.class,
                () -> userService.disableUser(id)
        );

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userMapper, never()).toResponse(any());
    }

    // ENABLE USER

    @Test
    void enableUser_shouldSetStatusToActive() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DISABLED);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponse(user))
                .thenReturn(response);

        UserResponse result = userService.enableUser(id);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(result);

        verify(keycloakUserService).setEnabled(user.getKeycloakId(), true);
        verify(userRepository).findById(id);
        verify(userMapper).toResponse(user);
    }

    @Test
    void enableUser_shouldThrowWhenUserNotFound() {
        Long id = 99L;

        when(userRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.enableUser(id)
        );

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void enableUser_shouldRejectDeletedUser() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        assertThrows(
                UserDeletedException.class,
                () -> userService.enableUser(id)
        );

        assertEquals(UserStatus.DELETED, user.getStatus());

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    // DELETE USER - SOFT DELETE

    @Test
    void deleteUser_shouldSetStatusToDeleted() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        userService.deleteUser(id);

        assertEquals(UserStatus.DELETED, user.getStatus());

        verify(keycloakUserService).setEnabled(user.getKeycloakId(), false);
        verify(userRepository).findById(id);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_shouldThrowWhenUserNotFound() {
        Long id = 99L;

        when(userRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(id)
        );

        verifyNoInteractions(keycloakUserService);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_shouldRejectAlreadyDeletedUser() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);

        when(userRepository.findById(id))
                .thenReturn(Optional.of(user));

        assertThrows(
                UserDeletedException.class,
                () -> userService.deleteUser(id)
        );

        assertEquals(UserStatus.DELETED, user.getStatus());

        verifyNoInteractions(keycloakUserService);
        verify(userRepository, never()).delete(any(User.class));
    }

    private User createUser(Long id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setKeycloakId("keycloak-" + id);
        user.setStatus(status);
        return user;
    }
}
