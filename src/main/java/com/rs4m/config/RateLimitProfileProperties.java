package com.rs4m.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(prefix = "r4sm.rate.default")
public class RateLimitProfileProperties {
    private Map<String, BucketProfile> buckets;

    @Data
    public static class BucketProfile {
        private boolean enable;
        private String version;
        private Map<String, Bandwidth> bandwidths;
    }

    @Data
    public static class Bandwidth {
        private String id;
        private int limit = 10;
        private long duration = 1;
        private TimeUnit timeUnit = TimeUnit.MINUTES;
    }

    @AllArgsConstructor
    @Getter
    public enum TimeUnit {
        SECONDS("SECONDS"),
        MINUTES("MINUTES"),
        HOURS("HOURS"),
        DAYS("DAYS");

        final String value;
    }
}
