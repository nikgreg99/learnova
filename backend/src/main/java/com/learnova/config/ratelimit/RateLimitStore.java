package com.learnova.config.ratelimit;

import java.time.Duration;

public interface RateLimitStore {

    RateLimitDecision consume(String key, int limit, Duration window);
}
