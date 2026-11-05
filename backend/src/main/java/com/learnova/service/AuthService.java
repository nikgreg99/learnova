package com.learnova.service;

import com.learnova.dto.auth.AuthUserResponse;
import com.learnova.mapper.AuthMapper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthMapper authMapper;

    public AuthService(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    public AuthUserResponse getCurrentUser(Jwt jwt) {
        return authMapper.toResponse(jwt);
    }
}
