package com.rs4m.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RateLimitConfig.RateLimiterProperties.class)
public class RateLimitConfig {

    @Bean("proxyManagerMaster")
    public ProxyManager<String> proxyManager(@Autowired(required = false) RedissonClient redissonClient) {
        if (redissonClient == null) {
            throw new IllegalStateException("RedissonClient is required for rate limiting. Please add redisson-spring-boot-starter to your dependencies.");
        }
        CommandAsyncExecutor commandExecutor = ((Redisson) redissonClient).getCommandExecutor();
        return Bucket4jRedisson.casBasedBuilder(commandExecutor).build();
    }

    @Bean
    public ExpressionParser expressionParser() {
        return new SpelExpressionParser();
    }


    @Bean
    public BucketConfiguration defaultBucketConfiguration(RateLimiterProperties properties) {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(properties.getDefaultLimit())
                        .refillGreedy(properties.getDefaultLimit(),
                                Duration.of(properties.getDefaultDuration(),
                                        properties.getDefaultTimeUnit())))
                .build();
    }

    @Data
    @ConfigurationProperties(prefix = "rs4m.rate-limiter")
    public static class RateLimiterProperties {
        /**
         * Whether rate limiting is enabled.
         */
        private boolean enabled = true;

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
}

