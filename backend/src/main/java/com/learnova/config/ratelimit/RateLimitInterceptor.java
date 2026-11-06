package com.learnova.config.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String PUBLIC_REGISTRATION_PATH = "/api/v1/users";

    private final RateLimitStore store;
    private final RateLimitProperties properties;

    public RateLimitInterceptor(RateLimitStore store, RateLimitProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        boolean publicRegistration = "POST".equals(request.getMethod())
                && PUBLIC_REGISTRATION_PATH.equals(request.getRequestURI());

        String identity = publicRegistration ? "ip:" + request.getRemoteAddr() : authenticatedIdentity();
        if (identity == null) {
            identity = "ip:" + request.getRemoteAddr();
        }

        int limit = publicRegistration
                ? properties.getPublicRequestsPerMinute()
                : properties.getAuthenticatedRequestsPerMinute();
        RateLimitDecision decision = store.consume(identity, limit, WINDOW);
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
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
