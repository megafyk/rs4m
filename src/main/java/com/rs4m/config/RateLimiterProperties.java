package com.rs4m.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.temporal.ChronoUnit;

@Data
@ConfigurationProperties(prefix = "rs4m.rate-limiter")
public class RateLimiterProperties {
    /**
     * Whether rate limiting is enabled.
     */
    private boolean enable = true;

    /**
     * Default limit for all rate limiters if not specified in annotation
     */
    private int defaultLimit = 20;

    /**
     * Default duration for the rate limit window if not specified in annotation
     */
    private long defaultDuration = 1;

    /**
     * Default time unit for the rate limit window if not specified in annotation
     */
    private ChronoUnit defaultTimeUnit = ChronoUnit.HOURS;
}
