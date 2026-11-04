
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

    public UserResponse disableUser(Long id) {
        User user = getUserOrThrow(id);
        ensureUserNotDeleted(user);
        keycloakUserService.setEnabled(user.getKeycloakId(), false);
        user.setStatus(UserStatus.DISABLED);

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse enableUser(Long id) {
        User user = getUserOrThrow(id);

        ensureUserNotDeleted(user);
        keycloakUserService.setEnabled(user.getKeycloakId(), true);
        user.setStatus(UserStatus.ACTIVE);

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
