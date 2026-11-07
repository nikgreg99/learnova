package com.learnova.config.ratelimit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    RateLimitStore rateLimitStore(StringRedisTemplate redisTemplate) {
        return new RedisRateLimitStore(redisTemplate);
    }

    @Bean
    RateLimitInterceptor rateLimitInterceptor(RateLimitStore store, RateLimitProperties properties) {
        return new RateLimitInterceptor(store, properties);
    }
}
