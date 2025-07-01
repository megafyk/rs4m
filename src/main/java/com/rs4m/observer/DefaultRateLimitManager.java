package com.rs4m.observer;


import com.rs4m.annotation.RateLimiter;
import com.rs4m.config.RateLimitProfileProperties;
import com.rs4m.config.RateLimitProfileProperties.Bandwidth;
import com.rs4m.config.RateLimitProfileProperties.BucketProfile;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConfigurationBuilder;
import io.github.bucket4j.TokensInheritanceStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component("defaultRateLimitManager")
public class DefaultRateLimitManager implements RateLimitManager, Subscriber<Map<String, BucketProfile>> {

    private final ProxyManager<String> proxyManager;
    private final ConcurrentMap<String, BucketPack> bucketConfigs;
    private final RateLimitProfileProperties rateLimitProfileProperties;

    @Autowired
    public DefaultRateLimitManager(ProxyManager<String> proxyManager, RateLimitProfileProperties rateLimitProfileProperties) {
        this.proxyManager = proxyManager;
        this.rateLimitProfileProperties = rateLimitProfileProperties;
        this.bucketConfigs = new ConcurrentHashMap<>();
    }

    @PostConstruct
    private void loadBucketConfigsFromYaml() {
        if (rateLimitProfileProperties.getBuckets() != null) {
            rateLimitProfileProperties.getBuckets().forEach((bucketCfgName, bucketProfile) -> {
                if (bucketProfile != null && bucketProfile.getBandwidths() != null && bucketProfile.isEnable()) {
                    BucketPack pack = BucketPack.builder().name(bucketCfgName).enabled(true).version(bucketProfile.getVersion()).bucketConfiguration(getBucketConfiguration(bucketProfile.getBandwidths())).build();
                    bucketConfigs.put(bucketCfgName, pack);
                }
            });
        }
    }

    private BucketConfiguration getBucketConfiguration(Map<String, Bandwidth> bandwidths) {
        ConfigurationBuilder builder = BucketConfiguration.builder();
        for (RateLimitProfileProperties.Bandwidth bandwidth : bandwidths.values()) {
            long tokens = bandwidth.getLimit();
            Duration period = Duration.of(bandwidth.getDuration(), ChronoUnit.valueOf(bandwidth.getTimeUnit().getValue()));
            builder.addLimit(limit -> limit.capacity(tokens).refillGreedy(tokens, period));
        }
        return builder.build();
    }

    @Override
    public void update(Map<String, BucketProfile> event) {
        for (Map.Entry<String, BucketProfile> bucketEntry : event.entrySet()) {
            String bucketName = bucketEntry.getKey();
            BucketProfile bucketProfile = bucketEntry.getValue();

            BucketPack pack = bucketConfigs.get(bucketName);
            if (pack == null && bucketProfile.isEnable()) {
                log.warn("No existing bucket configuration found for '{}'. Creating new configuration.", bucketName);
                BucketPack newPack = BucketPack.builder().name(bucketName).enabled(true).version(bucketProfile.getVersion()).bucketConfiguration(getBucketConfiguration(bucketProfile.getBandwidths())).build();
                bucketConfigs.put(bucketName, newPack);
            } else {
                if (bucketProfile.isEnable()) {
                    if (!pack.getVersion().equals(bucketProfile.getVersion())) {
                        log.info("Updating existing bucket configuration for '{}'.", bucketName);
                        log.info("Version changed from {} to {}. Updating configuration.", pack.getVersion(), bucketProfile.getVersion());
                        pack.setVersion(bucketProfile.getVersion());
                        pack.setBucketConfiguration(getBucketConfiguration(bucketProfile.getBandwidths()));
                    }
                } else {
                    bucketConfigs.remove(bucketName);
                }
            }
        }
    }


    public Bucket getBucket(String key, RateLimiter rateLimiter) {
        // Create bucket configuration from annotation
        BucketConfiguration bucketConfig = getBucketConfiguration(rateLimiter);
        Bucket bucket = proxyManager.builder().build(key, () -> bucketConfig);
        proxyManager.getProxyConfiguration(key).ifPresent(config -> {
            // If the bucket already exists, we can update its configuration
            if (!config.equals(bucketConfig)) {
                log.info("Updating bucket configuration for key '{}'.", key);
                // check if bucket configuration changed, call replace configuration
                bucket.replaceConfiguration(bucketConfig, TokensInheritanceStrategy.AS_IS);
            }
        });
        return bucket;
    }

    private BucketConfiguration getBucketConfiguration(RateLimiter rateLimiter) {
        if (bucketConfigs.containsKey(rateLimiter.value())) {
            return bucketConfigs.get(rateLimiter.value()).getBucketConfiguration();
        }
        // no bucket exists because of no bucket configuration or bucket already disabled
        throw new IllegalStateException("No bucket configuration found for key: " + rateLimiter.value());
    }
}
