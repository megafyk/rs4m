package com.rs4m.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(RedissonClusterConfigurationProperties.class)
public class RedisConfig {
    @Bean(name = "redisson", destroyMethod = "shutdown")
    public RedissonClient redisson(RedissonClusterConfigurationProperties prop) {
        Config config = new Config();
        config.useClusterServers()
                .setCheckSlotsCoverage(prop.isCheckSlotsCoverage())
                .setIdleConnectionTimeout(prop.getIdleConnectTimeout())
                .setConnectTimeout(prop.getConnectTimeout())
                .setTimeout(prop.getTimeout())
                .setRetryAttempts(prop.getRetryAttempts())
                .setFailedSlaveReconnectionInterval(prop.getFailedSlaveReconnectionInterval())
                .setPassword(prop.getPassword())
                .setSubscriptionsPerConnection(prop.getSubscriptionsPerConnection())
                .setClientName(prop.getClientName())
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
        return Redisson.create(config);
    }
}
