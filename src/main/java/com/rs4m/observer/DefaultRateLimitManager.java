package com.rs4m.observer;


import com.rs4m.annotation.RateLimiter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component("defaultRateLimitManager")
public class DefaultRateLimitManager implements RateLimitManager {

    private final ProxyManager<String> proxyManager;

    public DefaultRateLimitManager(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public void update(Object event) {
        log.info("update event: {}", event);
    }

    public Bucket getBucket(String key, RateLimiter rateLimiter) {
        // Create bucket configuration from annotation
        BucketConfiguration bucketConfig = getBucketConfiguration(rateLimiter);
        return proxyManager.builder().build(key, () -> bucketConfig);
    }

    @Override
    public String getId() {
        return "";
    }

    private BucketConfiguration getBucketConfiguration(RateLimiter rateLimiter) {
        long tokens = rateLimiter.limit();
        Duration period = Duration.of(rateLimiter.duration(), rateLimiter.unit());
        return BucketConfiguration.builder().addLimit(limit -> limit.capacity(tokens).refillGreedy(tokens, period)).build();
    }
}
