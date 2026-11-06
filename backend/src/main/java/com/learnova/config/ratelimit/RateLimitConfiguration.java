package com.learnova.config.ratelimit;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration implements WebMvcConfigurer {

    private final RateLimitInterceptor interceptor;

    public RateLimitConfiguration(RateLimitInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Bean
    RateLimitStore rateLimitStore() {
        return new InMemoryRateLimitStore(Clock.systemUTC());
    }

    @Bean
    RateLimitInterceptor rateLimitInterceptor(RateLimitStore store, RateLimitProperties properties) {
        return new RateLimitInterceptor(store, properties);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/**");
    }
}
