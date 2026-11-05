package com.learnova.mapper;

import com.learnova.dto.auth.AuthUserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.oauth2.jwt.Jwt;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", source = "subject")
    @Mapping(target = "username", expression = "java(jwt.getClaimAsString(\"preferred_username\"))")
    @Mapping(target = "email", expression = "java(jwt.getClaimAsString(\"email\"))")
    @Mapping(target = "firstName", expression = "java(jwt.getClaimAsString(\"given_name\"))")
    @Mapping(target = "lastName", expression = "java(jwt.getClaimAsString(\"family_name\"))")
    AuthUserResponse toResponse(Jwt jwt);
}
