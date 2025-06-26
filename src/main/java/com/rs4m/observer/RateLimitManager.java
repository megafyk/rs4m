package com.rs4m.observer;

import com.rs4m.annotation.RateLimiter;
import io.github.bucket4j.Bucket;

public interface RateLimitManager {
    Bucket getBucket(String clientKey, RateLimiter rateLimiter);
}
