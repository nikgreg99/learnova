package com.learnova.mapper;

import com.learnova.dto.user.UserRequest;
import com.learnova.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toEntity_shouldMapUsernameFromRequest() {
        UserRequest request = new UserRequest(
                "Mario",
                "Rossi",
                "mario.rossi@example.com",
                "mrossi",
                Set.of("STUDENT"),
                "SecurePassword123!"
        );

        User user = userMapper.toEntity(request);

        assertThat(user.getUsername()).isEqualTo("mrossi");
    }

    @Test
    void toResponse_shouldMapUsernameFromUser() {
        User user = new User();
        user.setUsername("mrossi");

        assertThat(userMapper.toResponse(user).username())
                .isEqualTo("mrossi");
    }
}
