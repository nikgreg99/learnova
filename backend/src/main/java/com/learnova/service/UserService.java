package com.learnova.service;


import com.learnova.dto.user.UserRequest;
import com.learnova.dto.user.UserResponse;
import com.learnova.exception.UserNotFoundException;
import com.learnova.mapper.UserMapper;
import com.learnova.model.Role;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import com.learnova.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakUserService keycloakUserService;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            KeycloakUserService keycloakUserService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.keycloakUserService = keycloakUserService;
    }

    public UserResponse createUser(UserRequest request) {

        // Create the identity in Keycloak
        String keycloakId = keycloakUserService.createUser(request);

        // Map the request DTO to the application entity
        User user = userMapper.toEntity(request);

        user.setKeycloakId(keycloakId);

        Role role = request.role() == null
                ? Role.STUDENT
                : request.role();

        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);

        // Persist the application profile
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

}
