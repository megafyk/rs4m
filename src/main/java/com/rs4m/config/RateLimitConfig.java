package com.rs4m.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimitConfig {

    @Bean("proxyManagerMaster")
    public ProxyManager<String> proxyManager(@Autowired(required = false) RedissonClient redissonClient) {
        if (redissonClient == null) {
            throw new IllegalStateException("RedissonClient must be configured for rate limiting to work");
        }
        CommandAsyncExecutor commandExecutor = ((Redisson) redissonClient).getCommandExecutor();
        return RedissonBasedProxyManager.builderFor(commandExecutor).build();
    }

    @Bean
    public ExpressionParser expressionParser() {
        return new SpelExpressionParser();
    }


    @Bean
    public BucketConfiguration defaultBucketConfiguration(RateLimiterProperties properties) {
        return BucketConfiguration.builder().addLimit(limit -> limit.capacity(properties.getDefaultLimit()).refillGreedy(properties.getDefaultLimit(), Duration.of(properties.getDefaultDuration(), properties.getDefaultTimeUnit()))).build();
    }

}

