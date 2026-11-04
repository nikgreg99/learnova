
package com.learnova.service;

import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakUserService keycloakUserService;
    private final RoleRepository roleRepository;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            KeycloakUserService keycloakUserService,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.keycloakUserService = keycloakUserService;
        this.roleRepository = roleRepository;
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void ensureUserNotDeleted(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new UserDeletedException(user.getId());
        }
    }

    public UserResponse createUser(UserRequest request) {

        // Create the identity in Keycloak
        String keycloakId = keycloakUserService.createUser(request);

        // Map the request DTO to the application entity
        User user = userMapper.toEntity(request);

        user.setKeycloakId(keycloakId);

        // Assign roles from the database
        Set<String> requestedRoles = request.roles() == null
                || request.roles().isEmpty()
                ? Set.of("STUDENT")
                : request.roles();

        Set<Role> roles = new HashSet<>(
                roleRepository.findAllByNameIn(requestedRoles)
        );

        if (roles.size() != requestedRoles.size()) {
            throw new IllegalArgumentException(
                    "One or more requested roles do not exist"
            );
        }

        user.setRoles(roles);
        user.setStatus(UserStatus.ACTIVE);

        // Persist the application profile
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public Page<UserResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    public UserResponse getUser(Long id) {
        return userMapper.toResponse(getUserOrThrow(id));
    }

    public UserResponse updateUserStatus(Long id, UserStatus status) {
        if (status == null || status == UserStatus.DELETED
                || status == UserStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Invalid user status");
        }

        User user = getUserOrThrow(id);
        ensureUserNotDeleted(user);
        keycloakUserService.setEnabled(
                user.getKeycloakId(),
                status == UserStatus.ACTIVE
        );
        user.setStatus(status);

        return userMapper.toResponse(user);
    }

    public UserResponse updateUserProfile(
            Long id,
            UserProfileUpdateRequest request
    ) {
        User user = getUserOrThrow(id);
        ensureUserNotDeleted(user);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new UserProfileConflictException("email");
        }
        if (userRepository.existsByUsernameAndIdNot(request.username(), id)) {
            throw new UserProfileConflictException("username");
        }
        keycloakUserService.updateProfile(user.getKeycloakId(), request);
        userMapper.updateUserProfile(request, user);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        ensureUserNotDeleted(user);
        keycloakUserService.setEnabled(user.getKeycloakId(), false);
        user.setStatus(UserStatus.DELETED);
    }

}
