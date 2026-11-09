package com.learnova.config.ratelimit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisRateLimitStoreTest {

    private StringRedisTemplate redisTemplate;
    private RedisRateLimitStore store;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        store = new RedisRateLimitStore(redisTemplate);
    }

    @Test
    void consume_shouldPermitWhenRetryAfterIsZero() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), eq("60000"), eq("10")))
                .thenReturn(0L);

        RateLimitDecision decision = store.consume("test-key", 10, Duration.ofMinutes(1));

        assertTrue(decision.allowed());
        assertEquals(0, decision.retryAfterSeconds());
    }

    @Test
    void consume_shouldRejectWhenLimitIsExceeded() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), eq("60000"), eq("10")))
                .thenReturn(15000L);

        RateLimitDecision decision = store.consume("test-key", 10, Duration.ofMinutes(1));

        assertFalse(decision.allowed());
        assertEquals(15, decision.retryAfterSeconds());
    }

    @Test
    void consume_shouldFailOpenWhenRedisThrowsException() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), eq("60000"), eq("10")))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        RateLimitDecision decision = store.consume("test-key", 10, Duration.ofMinutes(1));

        assertTrue(decision.allowed());
        assertEquals(0, decision.retryAfterSeconds());
    }

    @Test
    void consume_shouldThrowIllegalArgumentExceptionOnInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> store.consume("key", 0, Duration.ofMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> store.consume("key", 10, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> store.consume("key", 10, Duration.ofSeconds(-5)));
    }
}
