package com.rs4m.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * Annotation for rate limiting controller methods or entire controllers.
 * When applied to a controller class, all handler methods will be rate limited.
 * When applied to a method, only that method will be rate limited.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * Maximum number of requests allowed within the specified time window.
     *
     * @return the limit of requests
     */
    int limit() default 20;

    /**
     * The duration of the window in which the requests are counted.
     *
     * @return the duration value
     */
    long duration() default 1;

    /**
     * The time unit of the duration.
     *
     * @return the time unit
     */
    ChronoUnit unit() default ChronoUnit.MINUTES;

    /**
     * Key resolver strategy to use for identifying clients.
     * Available strategies:
     * - IP: Uses the client's IP address
     * - HEADER: Uses a specified HTTP header value
     * - EXPRESSION: Uses a SpEL expression
     *
     * @return the key resolver strategy
     */
    KeyResolver keyResolver() default KeyResolver.IP;

    /**
     * The name of the header to use when keyResolver is set to HEADER.
     *
     * @return the header name
     */
    String headerName() default "X-API-KEY";

    /**
     * SpEL expression to evaluate when keyResolver is set to EXPRESSION.
     * The expression has access to 'request' (HttpServletRequest) variable.
     *
     * @return the SpEL expression
     */
    String keyExpression() default "";

    /**
     * Enumeration of key resolver strategies.
     */
    enum KeyResolver {
        /**
         * Use the client's IP address as the key
         */
        IP,

        /**
         * Use a specified HTTP header value as the key
         */
        HEADER,

        /**
         * Use a SpEL expression to determine the key
         */
        EXPRESSION
    }
}
