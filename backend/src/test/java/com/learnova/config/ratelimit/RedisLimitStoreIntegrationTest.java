package com.learnova.config.ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisRateLimitStoreIntegrationTest {

    private static final int RATE_LIMIT = 3;

    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static GenericContainer<?> redisContainer;

    private static LettuceConnectionFactory connectionFactory;

    private static RedisRateLimitStore rateLimitStore;

    @BeforeAll
    static void setUp() {
        redisContainer = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
        ).withExposedPorts(6379);

        redisContainer.start();

        connectionFactory = new LettuceConnectionFactory(
            redisContainer.getHost(),
            redisContainer.getMappedPort(6379)
        );

        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate =
            new StringRedisTemplate();

        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        rateLimitStore = new RedisRateLimitStore(redisTemplate);
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }

        if (redisContainer != null) {
            redisContainer.stop();
        }
    }

    @Test
    void consume_shouldAllowRequestsWithinLimit() {
        String key = uniqueKey();

        var first = rateLimitStore.consume(key, RATE_LIMIT, WINDOW);
        var second = rateLimitStore.consume(key, RATE_LIMIT, WINDOW);
        var third = rateLimitStore.consume(key, RATE_LIMIT, WINDOW);

        assertTrue(first.allowed());
        assertTrue(second.allowed());
        assertTrue(third.allowed());
    }

    @Test
    void consume_shouldRejectRequestsWhenLimitIsExceeded() {
        String key = uniqueKey();

        rateLimitStore.consume(key, RATE_LIMIT, WINDOW);
        rateLimitStore.consume(key, RATE_LIMIT, WINDOW);
        rateLimitStore.consume(key, RATE_LIMIT, WINDOW);

        var fourth = rateLimitStore.consume(key, RATE_LIMIT, WINDOW);

        assertFalse(fourth.allowed());
        assertTrue(fourth.retryAfterSeconds() > 0);
    }

    @Test
    void consume_shouldKeepDifferentBucketsIndependent() {
        String firstKey = uniqueKey();
        String secondKey = uniqueKey();

        for (int i = 0; i < RATE_LIMIT; i++) {
            rateLimitStore.consume(firstKey, RATE_LIMIT, WINDOW);
        }

        var rejected = rateLimitStore.consume(
            firstKey, RATE_LIMIT, WINDOW
        );

        var allowed = rateLimitStore.consume(
            secondKey, RATE_LIMIT, WINDOW
        );

        assertFalse(rejected.allowed());
        assertTrue(allowed.allowed());
    }

    @Test
    void consume_shouldAllowRequestsAgainAfterWindowExpires()
        throws InterruptedException {

        String key = uniqueKey();
        Duration shortWindow = Duration.ofMillis(300);

        for (int i = 0; i < RATE_LIMIT; i++) {
            rateLimitStore.consume(key, RATE_LIMIT, shortWindow);
        }

        var rejected = rateLimitStore.consume(
            key, RATE_LIMIT, shortWindow
        );

        assertFalse(rejected.allowed());

        Thread.sleep(500);

        var allowed = rateLimitStore.consume(
            key, RATE_LIMIT, shortWindow
        );

        assertTrue(allowed.allowed());
    }

    @Test
    void consume_shouldRejectInvalidLimit() {
        assertThrows(
            IllegalArgumentException.class,
            () -> rateLimitStore.consume(
                uniqueKey(), 0, WINDOW
            )
        );
    }

    @Test
    void consume_shouldRejectInvalidWindow() {
        assertThrows(
            IllegalArgumentException.class,
            () -> rateLimitStore.consume(
                uniqueKey(), RATE_LIMIT, Duration.ZERO
            )
        );
    }

    private String uniqueKey() {
        return "password-reset:ip:" + UUID.randomUUID();
    }
}
