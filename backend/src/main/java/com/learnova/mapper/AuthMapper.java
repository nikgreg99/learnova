package com.learnova.mapper;


import com.learnova.dto.auth.AuthUserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", source = "subject")
    @Mapping(target = "username", source = "preferredUsername")
    @Mapping(target = "firstName", source = "givenName")
    @Mapping(target = "lastName", source = "familyName")
    AuthUserResponse toResponse(OidcUser oidcUser);
}