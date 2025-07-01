package com.rs4m.config;

import com.rs4m.filter.RateLimiterFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for RS4M rate limiting library.
 * This configuration is automatically loaded when the library is on the classpath.
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "rs4m.rate", name = "enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({RateLimiterProperties.class, RateLimitProfileProperties.class})
@ComponentScan(basePackages = {
    "com.rs4m.filter",
    "com.rs4m.observer",
    "com.rs4m.api"
})
@Import({RateLimitConfig.class, RedisConfig.class})
public class Rs4mAutoConfiguration {

    /**
     * Register the RateLimiterFilter with high priority to ensure it runs early in the filter chain.
     */
    @Bean
    public FilterRegistrationBean<RateLimiterFilter> rateLimiterFilterRegistration(RateLimiterFilter rateLimiterFilter) {
        FilterRegistrationBean<RateLimiterFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimiterFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // High priority
        registration.setName("rateLimiterFilter");
        return registration;
    }
}
