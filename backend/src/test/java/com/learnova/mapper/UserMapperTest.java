package com.learnova.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnova.dto.user.UserProfileUpdateRequest;
import com.learnova.dto.user.UserRequest;
import com.learnova.model.User;
import com.learnova.model.UserStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toEntity_shouldMapUsernameFromRequest() {
        UserRequest request = new UserRequest("Mario", "Rossi", "mario.rossi@example.com", "mrossi", Set.of("STUDENT"),
                "SecurePassword123!");

        User user = userMapper.toEntity(request);

        assertThat(user.getUsername()).isEqualTo("mrossi");
    }

    @Test
    void toResponse_shouldMapUsernameFromUser() {
        User user = new User();
        user.setUsername("mrossi");

        assertThat(userMapper.toResponse(user).username()).isEqualTo("mrossi");
    }

    @Test
    void updateUserProfile_shouldUpdateProfileAndPreserveOtherFields() {
        User user = new User();
        user.setKeycloakId("keycloak-123");
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");
        user.setUsername("mrossi");
        user.setStatus(UserStatus.ACTIVE);

        UserProfileUpdateRequest request = new UserProfileUpdateRequest("Updated", "User", "updated@example.com",
                "updated");

        userMapper.updateUserProfile(request, user);

        assertThat(user.getFirstName()).isEqualTo("Updated");
        assertThat(user.getLastName()).isEqualTo("User");
        assertThat(user.getEmail()).isEqualTo("updated@example.com");
        assertThat(user.getUsername()).isEqualTo("updated");
        assertThat(user.getKeycloakId()).isEqualTo("keycloak-123");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
