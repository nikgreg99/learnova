package com.learnova.service;

import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.mapper.AuthMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthMapper authMapper;

    public AuthService(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    public AuthUserResponse getCurrentUser(OidcUser oidcUser) {
        return authMapper.toResponse(oidcUser);
    }
}
