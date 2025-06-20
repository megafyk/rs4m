package com.rs4m.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "rs4m.redisson")
public class RedissonClusterConfigurationProperties {
    // redis cluster default config
    private boolean enabled = true;
    private boolean checkSlotsCoverage = true;
    private int idleConnectTimeout = 10000;
    private int connectTimeout = 10000;
    private int timeout = 3000;
    private int retryAttempts = 3;
    private int failedSlaveReconnectionInterval = 3000;
    private String password = null;
    private int subscriptionsPerConnection = 5;
    private String clientName = null;
    private int subscriptionConnectionPoolSize = 50;
    private int slaveConnectionMinimumIdleSize = 24;
    private int slaveConnectionPoolSize = 64;
    private int masterConnectionMinimumIdleSize = 24;
    private int masterConnectionPoolSize = 64;
    private String nodeAddresses = null;
    private int scanInterval = 1000;
    private int pingConnectionInterval = 30000;
    private boolean keepAlive = false;
    private boolean tcpNoDelay = false;
}
