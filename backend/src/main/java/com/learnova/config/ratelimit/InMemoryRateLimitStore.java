package com.learnova.config.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryRateLimitStore implements RateLimitStore {

    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(2);
    private static final long MAX_BUCKETS = 100_000;

    private final Cache<String, Window> windows = Caffeine.newBuilder().maximumSize(MAX_BUCKETS)
            .expireAfterAccess(CACHE_EXPIRY).build();
    private final Clock clock;

    public InMemoryRateLimitStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public RateLimitDecision consume(String key, int limit, Duration window) {
        if (limit < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit and window must be positive.");
        }

        long now = clock.millis();
        long windowMillis = window.toMillis();
        AtomicReference<RateLimitDecision> decision = new AtomicReference<>();

        windows.asMap().compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAtMillis() >= windowMillis) {
                decision.set(RateLimitDecision.permit());
                return new Window(now, 1);
            }

            if (current.requestCount() >= limit) {
                long remainingMillis = windowMillis - (now - current.startedAtMillis());
                long retryAfterSeconds = (remainingMillis + 999) / 1000;
                decision.set(RateLimitDecision.reject(retryAfterSeconds));
                return current;
            }

            decision.set(RateLimitDecision.permit());
            return new Window(current.startedAtMillis(), current.requestCount() + 1);
        });

        return decision.get();
    }

    private record Window(
            long startedAtMillis,
            int requestCount) {
    }
}
