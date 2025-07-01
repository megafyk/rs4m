package com.rs4m.config;

import com.rs4m.filter.RateLimiterFilter;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.time.Duration;
import java.util.Arrays;

/**
 * Auto-configuration for RS4M rate limiting library.
 * This configuration is automatically loaded when the library is on the classpath.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "rs4m.rate", name = "enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({RateLimiterProperties.class, RateLimitProfileProperties.class, Rs4mRedissonProperties.class})
@ComponentScan(basePackages = {
        "com.rs4m.filter",
        "com.rs4m.observer",
        "com.rs4m.api"
})
@EnableCaching
public class Rs4mAutoConfig {
    @Bean("proxyManagerMaster")
    @ConditionalOnProperty(prefix = "rs4m.rate", name = "enable", havingValue = "true", matchIfMissing = true)
    public ProxyManager<String> proxyManager(@Qualifier("redissonRs4m") RedissonClient redissonClient) {
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

    /**
     * Register the RateLimiterFilter with high priority to ensure it runs early in the filter chain.
     */
    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilterRegistration(RateLimiterFilter rateLimiterFilter) {
        FilterRegistrationBean<RateLimiterFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimiterFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(0); // High priority
        registration.setName("rateLimiterFilter");
        return registration;
    }

    @Bean(name = "redissonRs4m", destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "rs4m.rate", name = "enable", havingValue = "true", matchIfMissing = true)
    public RedissonClient redisson(Rs4mRedissonProperties prop) {
        if (prop.getNodeAddresses() == null || prop.getNodeAddresses().trim().isEmpty()) {
            throw new IllegalStateException("Redis node addresses must be configured. Please set 'rs4m.redisson.nodeAddresses' property.");
        }

        Config config = new Config();
        config.useClusterServers()
                .setCheckSlotsCoverage(prop.isCheckSlotsCoverage())
                .setIdleConnectionTimeout(prop.getIdleConnectTimeout())
                .setConnectTimeout(prop.getConnectTimeout())
                .setTimeout(prop.getTimeout())
                .setRetryAttempts(prop.getRetryAttempts())
                .setFailedSlaveReconnectionInterval(prop.getFailedSlaveReconnectionInterval())
                .setSubscriptionsPerConnection(prop.getSubscriptionsPerConnection())
                .setSubscriptionConnectionPoolSize(prop.getSubscriptionConnectionPoolSize())
                .setSlaveConnectionMinimumIdleSize(prop.getSlaveConnectionMinimumIdleSize())
                .setSlaveConnectionPoolSize(prop.getSlaveConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(prop.getMasterConnectionMinimumIdleSize())
                .setMasterConnectionPoolSize(prop.getMasterConnectionPoolSize())
                .setScanInterval(prop.getScanInterval())
                .setPingConnectionInterval(prop.getPingConnectionInterval())
                .setKeepAlive(prop.isKeepAlive())
                .setTcpNoDelay(prop.isTcpNoDelay())
                .setNodeAddresses(Arrays.asList(prop.getNodeAddresses().split(",")));

        // Set password only if it's not null or empty
        if (prop.getPassword() != null && !prop.getPassword().trim().isEmpty()) {
            config.useClusterServers().setPassword(prop.getPassword());
        }

        // Set client name only if it's not null or empty
        if (prop.getClientName() != null && !prop.getClientName().trim().isEmpty()) {
            config.useClusterServers().setClientName(prop.getClientName());
        }

        return Redisson.create(config);
    }
}
