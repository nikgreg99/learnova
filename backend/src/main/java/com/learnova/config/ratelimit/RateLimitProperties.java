package com.learnova.config.ratelimit;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learnova.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private int authenticatedRequestsPerMinute = 100;
    private int publicRequestsPerMinute = 10;
    private int anonymousRequestsPerMinute = 60;
    private Duration window = Duration.ofMinutes(1);
}
