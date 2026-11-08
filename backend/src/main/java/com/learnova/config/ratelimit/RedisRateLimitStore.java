package com.learnova.config.ratelimit;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@RequiredArgsConstructor
public class RedisRateLimitStore implements RateLimitStore {

    private static final String KEY_PREFIX = "learnova:rate-limit:";
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 or redis.call('PTTL', KEYS[1]) < 0 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            if count > tonumber(ARGV[2]) then
                return redis.call('PTTL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public RateLimitDecision consume(String key, int limit, Duration window) {
        if (limit < 1 || window.isZero() || window.isNegative() || window.toMillis() < 1) {
            throw new IllegalArgumentException("Rate limit and window must be positive.");
        }

        long retryAfterMillis = redisTemplate.execute(CONSUME_SCRIPT, List.of(KEY_PREFIX + key),
                Long.toString(window.toMillis()), Integer.toString(limit));
        if (retryAfterMillis <= 0) {
            return RateLimitDecision.permit();
        }

        return RateLimitDecision.reject((retryAfterMillis + 999) / 1000);
    }
}
