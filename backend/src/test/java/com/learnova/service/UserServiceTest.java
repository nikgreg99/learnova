
package com.learnova.service;

import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.exception.UserDeletedException;
import com.learnova.exception.UserNotFoundException;
import com.learnova.exception.UserProfileConflictException;
import com.learnova.mapper.UserMapper;
import com.learnova.model.Role;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import com.learnova.repository.RoleRepository;
import com.learnova.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private UserResponse response;

    @BeforeEach
    void setUp() {
        request = new UserRequest("Mario", "Rossi", "mario.rossi@example.com", "mrossi",
                Set.of("STUDENT", "INSTRUCTOR"), "SecurePassword123!");

        user = new User();
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");

        studentRole = Role.builder().id(1L).name("STUDENT").description("Student").build();

        response = new UserResponse(1L, "Mario", "Rossi", "mario.rossi@example.com", "mrossi", Set.of("STUDENT"),
                UserStatus.ACTIVE, null, null);
    }

    // CREATE USER
    @Test
    void createUser_shouldAlwaysAssignStudentRole() {
        when(keycloakUserService.createUser(request)).thenReturn("keycloak-123");

        when(userMapper.toEntity(request)).thenReturn(user);

        // Now it always looks up Set.of("STUDENT")!
        when(roleRepository.findAllByNameIn(Set.of("STUDENT"))).thenReturn(List.of(studentRole));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        verify(roleRepository).findAllByNameIn(Set.of("STUDENT"));
    }

    @Test
    void createUser_whenDatabaseSaveFails_shouldDeleteKeycloakUserAndRethrow() {
        when(keycloakUserService.createUser(request)).thenReturn("keycloak-123");
        when(userMapper.toEntity(request)).thenReturn(user);
        when(roleRepository.findAllByNameIn(request.roles())).thenReturn(List.of(studentRole));
        // Simulate database failure on save
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection lost"));

        // 1. Assert that the exception is re-thrown to the caller
        assertThrows(RuntimeException.class, () -> userService.createUser(request));

        // 2. Assert that Keycloak cleanup was invoked with the exact keycloak-123 ID!
        verify(keycloakUserService).deleteUser("keycloak-123");
    }

    @Test
    void getUsers_shouldReturnPaginatedUserResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        User firstUser = createUser(1L, UserStatus.ACTIVE);
        User secondUser = createUser(2L, UserStatus.DISABLED);
        Page<User> users = new PageImpl<>(List.of(firstUser, secondUser), pageable, 2);

        when(userRepository.findAll(pageable)).thenReturn(users);
        when(userMapper.toResponse(firstUser)).thenReturn(response);
        when(userMapper.toResponse(secondUser)).thenReturn(response);

        Page<UserResponse> result = userService.getUsers(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getUser_shouldReturnUserResponse() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        assertEquals(response, userService.getUser(id));
        verify(userRepository).findById(id);
    }

    @Test
    void getUser_shouldThrowWhenUserNotFound() {
        Long id = 99L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUser(id));
        verify(userMapper, never()).toResponse(any());
    }

    // UPDATE USER STATUS
    @Test
    void updateUserStatus_shouldSetStatusToDisabled() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUserStatus(id, UserStatus.DISABLED);

        assertEquals(UserStatus.DISABLED, user.getStatus());
        assertNotNull(result);

        verify(keycloakUserService).setEnabled(user.getKeycloakId(), false);
        verify(userRepository).findById(id);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserStatus_shouldThrowWhenUserNotFoundForDisabledStatus() {
        Long id = 99L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUserStatus(id, UserStatus.DISABLED));

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserStatus_shouldRejectUnsupportedStatus() {
        assertThrows(IllegalArgumentException.class, () -> userService.updateUserStatus(1L, UserStatus.DELETED));

        verifyNoInteractions(userRepository, keycloakUserService, userMapper);
    }

    @Test
    void updateUserProfile_shouldUpdateKeycloakAndLocalProfile() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);
        UserProfileUpdateRequest update = new UserProfileUpdateRequest("Updated", "User", "updated@example.com",
                "updated");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUserProfile(id, update);

        assertNotNull(result);
        verify(keycloakUserService).updateProfile(user.getKeycloakId(), update);
        verify(userMapper).updateUserProfile(update, user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserProfile_shouldRejectEmailOrUsernameAlreadyInUse() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);
        UserProfileUpdateRequest update = new UserProfileUpdateRequest("Updated", "User", "taken@example.com",
                "updated");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot(update.email(), id)).thenReturn(true);

        UserProfileConflictException exception = assertThrows(UserProfileConflictException.class,
                () -> userService.updateUserProfile(id, update));

        assertEquals("email", exception.getConflictingField());
        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserProfile_shouldNotChangeLocalProfileWhenKeycloakUpdateFails() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);
        user.setFirstName("Original");
        UserProfileUpdateRequest update = new UserProfileUpdateRequest("Updated", "User", "updated@example.com",
                "updated");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Keycloak update failed")).when(keycloakUserService)
                .updateProfile(user.getKeycloakId(), update);

        assertThrows(IllegalStateException.class, () -> userService.updateUserProfile(id, update));

        assertEquals("Original", user.getFirstName());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserProfile_shouldRejectDeletedUser() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);
        UserProfileUpdateRequest update = new UserProfileUpdateRequest("Updated", "User", "updated@example.com",
                "updated");

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(UserDeletedException.class, () -> userService.updateUserProfile(id, update));

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserStatus_shouldRejectDeletedUserWithoutChangingKeycloak() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(UserDeletedException.class, () -> userService.updateUserStatus(id, UserStatus.DISABLED));

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserStatus_shouldKeepLocalStatusWhenKeycloakUpdateFails() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("Keycloak update failed")).when(keycloakUserService)
                .setEnabled(user.getKeycloakId(), false);

        assertThrows(IllegalStateException.class, () -> userService.updateUserStatus(id, UserStatus.DISABLED));

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserStatus_shouldSetStatusToActive() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DISABLED);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUserStatus(id, UserStatus.ACTIVE);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(result);

        verify(keycloakUserService).setEnabled(user.getKeycloakId(), true);
        verify(userRepository).findById(id);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserStatus_shouldThrowWhenUserNotFoundForActiveStatus() {
        Long id = 99L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUserStatus(id, UserStatus.ACTIVE));

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void updateUserStatus_shouldRejectDeletedUser() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(UserDeletedException.class, () -> userService.updateUserStatus(id, UserStatus.ACTIVE));

        assertEquals(UserStatus.DELETED, user.getStatus());

        verifyNoInteractions(keycloakUserService);
        verify(userMapper, never()).toResponse(any());
    }

    // DELETE USER - SOFT DELETE

    @Test
    void deleteUser_shouldSetStatusToDeleted() {
        Long id = 1L;
        User user = createUser(id, UserStatus.ACTIVE);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.deleteUser(id);

        assertEquals(UserStatus.DELETED, user.getStatus());

        verify(keycloakUserService).setEnabled(user.getKeycloakId(), false);
        verify(userRepository).findById(id);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_shouldThrowWhenUserNotFound() {
        Long id = 99L;

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(id));

        verifyNoInteractions(keycloakUserService);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUser_shouldRejectAlreadyDeletedUser() {
        Long id = 1L;
        User user = createUser(id, UserStatus.DELETED);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertThrows(UserDeletedException.class, () -> userService.deleteUser(id));

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
