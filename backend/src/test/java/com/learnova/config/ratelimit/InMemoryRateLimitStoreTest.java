package com.learnova.config.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class InMemoryRateLimitStoreTest {

    @Test
    void consume_shouldRejectRequestsAfterLimitAndAllowAgainAfterWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryRateLimitStore store = new InMemoryRateLimitStore(clock);
        Duration window = Duration.ofMinutes(1);

        assertTrue(store.consume("user:123", 2, window).allowed());
        assertTrue(store.consume("user:123", 2, window).allowed());

        RateLimitDecision rejected = store.consume("user:123", 2, window);
        assertFalse(rejected.allowed());
        assertTrue(rejected.retryAfterSeconds() > 0);

        clock.advance(window);

        assertTrue(store.consume("user:123", 2, window).allowed());
    }

    private static class MutableClock extends Clock {

        private final AtomicLong currentMillis;

        private MutableClock(Instant initialInstant) {
            currentMillis = new AtomicLong(initialInstant.toEpochMilli());
        }

        private void advance(Duration duration) {
            currentMillis.addAndGet(duration.toMillis());
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(currentMillis.get());
        }

        @Override
        public long millis() {
            return currentMillis.get();
        }
    }
}
