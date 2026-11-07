package com.learnova.config.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String PUBLIC_REGISTRATION_PATH = "/api/v1/users";

    private final RateLimitStore store;
    private final RateLimitProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean publicRegistration = "POST".equals(request.getMethod()) && PUBLIC_REGISTRATION_PATH.equals(path);
        String authenticatedUser = authenticatedIdentity();
        String bucket;
        int limit;

        if (publicRegistration) {
            bucket = "registration:ip:" + request.getRemoteAddr();
            limit = properties.getPublicRequestsPerMinute();
        } else if (authenticatedUser != null) {
            bucket = "authenticated:" + authenticatedUser;
            limit = properties.getAuthenticatedRequestsPerMinute();
        } else {
            bucket = "anonymous:ip:" + request.getRemoteAddr();
            limit = properties.getAnonymousRequestsPerMinute();
        }

        RateLimitDecision decision = store.consume(bucket, limit, properties.getWindow());
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"Rate limit exceeded.\"}");
        return false;
    }

    private String authenticatedIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return "user:" + jwtAuthentication.getToken().getSubject();
        }

        return "user:" + authentication.getName();
    }
}
