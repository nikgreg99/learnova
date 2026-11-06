package com.learnova.config.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitInterceptorTest {

    private final RateLimitStore store = mock(RateLimitStore.class);
    private final RateLimitProperties properties = new RateLimitProperties();
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(store, properties);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandle_shouldLimitPublicRegistrationByClientAddress() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/users", "192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(store.consume("ip:192.0.2.10", 10, Duration.ofMinutes(1))).thenReturn(RateLimitDecision.permit());

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(store).consume("ip:192.0.2.10", 10, Duration.ofMinutes(1));
    }

    @Test
    void preHandle_shouldLimitAuthenticatedRequestsByUser() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("user-123", "credentials", "ROLE_USER"));
        MockHttpServletRequest request = request("GET", "/api/v1/courses", "192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(store.consume("user:user-123", 100, Duration.ofMinutes(1))).thenReturn(RateLimitDecision.permit());

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verify(store).consume("user:user-123", 100, Duration.ofMinutes(1));
    }

    @Test
    void preHandle_shouldReturnTooManyRequestsAndRetryAfterWhenLimitIsExceeded() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/users", "192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(store.consume("ip:192.0.2.10", 10, Duration.ofMinutes(1))).thenReturn(RateLimitDecision.reject(42));

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(429, response.getStatus());
        assertEquals("42", response.getHeader("Retry-After"));
    }

    private MockHttpServletRequest request(String method, String path, String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(path);
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
