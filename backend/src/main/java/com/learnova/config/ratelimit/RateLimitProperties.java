package com.learnova.config.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "learnova.rate-limit")
public class RateLimitProperties {

    private int authenticatedRequestsPerMinute = 100;
    private int publicRequestsPerMinute = 10;

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
}
