
package com.learnova.config.ratelimit;

import java.time.Duration;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Slf4j
@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private static final String KEY_PREFIX = "learnova:rate-limit:";

    private static final DefaultRedisScript<Long> CONSUME_SCRIPT =
        new DefaultRedisScript<>();

    static {
        CONSUME_SCRIPT.setLocation(
            new ClassPathResource("scripts/rate-limit.lua")
        );
        CONSUME_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    @Override
    public RateLimitDecision consume(String key, int limit, Duration window) {
        if (limit < 1 || window.isZero() || window.isNegative()
            || window.toMillis() < 1) {
            throw new IllegalArgumentException(
                "Rate limit and window must be positive."
            );
        }

        try {
            long retryAfterMillis = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(KEY_PREFIX + key),
                Long.toString(window.toMillis()),
                Integer.toString(limit)
            );

            if (retryAfterMillis <= 0) {
                return RateLimitDecision.permit();
            }

            return RateLimitDecision.reject(
                (retryAfterMillis + 999) / 1000
            );

        } catch (Exception ex) {
            log.warn(
                "Redis rate limiter unavailable, failing open: {}",
                ex.getMessage()
            );
            return RateLimitDecision.permit();
        }
    }
}
