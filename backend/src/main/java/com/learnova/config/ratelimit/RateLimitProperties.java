package com.learnova.config.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learnova.rate-limit")
public class RateLimitProperties {

    private int authenticatedRequestsPerMinute = 100;
    private int publicRequestsPerMinute = 10;
    private int anonymousRequestsPerMinute = 60;
    private Duration window = Duration.ofMinutes(1);

    public int getAuthenticatedRequestsPerMinute() {
        return authenticatedRequestsPerMinute;
    }

    public void setAuthenticatedRequestsPerMinute(int authenticatedRequestsPerMinute) {
        this.authenticatedRequestsPerMinute = authenticatedRequestsPerMinute;
    }

    public int getPublicRequestsPerMinute() {
        return publicRequestsPerMinute;
    }

    public void setPublicRequestsPerMinute(int publicRequestsPerMinute) {
        this.publicRequestsPerMinute = publicRequestsPerMinute;
    }

    public int getAnonymousRequestsPerMinute() {
        return anonymousRequestsPerMinute;
    }

    public void setAnonymousRequestsPerMinute(int anonymousRequestsPerMinute) {
        this.anonymousRequestsPerMinute = anonymousRequestsPerMinute;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
