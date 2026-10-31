package com.learnova.mapper;

import com.learnova.dto.user.UserResponse;
import com.learnova.dto.user.UserRequest;
import com.learnova.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)

    User toEntity(UserRequest request);
    UserResponse toResponse(User user);
}