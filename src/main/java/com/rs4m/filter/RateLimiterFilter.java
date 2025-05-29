package com.rs4m.filter;

import com.rs4m.annotation.RateLimiter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * Rate limiter filter for HTTP requests based on the @RateLimiter annotation.
 */
@Slf4j
@Component
@AllArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ExpressionParser expressionParser = new SpelExpressionParser();


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Find handler method for this request
            HandlerMethod handlerMethod = (HandlerMethod) Objects.requireNonNull(handlerMapping.getHandler(request)).getHandler();

            // Check for @RateLimiter annotation on method
            RateLimiter rateLimiterAnnotation = handlerMethod.getMethodAnnotation(RateLimiter.class);

            // If not on method, check the controller class
            if (rateLimiterAnnotation == null) {
                rateLimiterAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), RateLimiter.class);
            }

            // If no annotation is found, continue with the filter chain
            if (rateLimiterAnnotation == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Apply rate limiting
            if (applyRateLimit(request, response, rateLimiterAnnotation)) {
                // Rate limit not exceeded, continue with the filter chain
                filterChain.doFilter(request, response);
            }
            // If rate limit exceeded, the response is already set by applyRateLimit method

        } catch (Exception e) {
            log.error("Error in rate limiter filter", e);
            // On error, continue with the filter chain
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Apply rate limiting based on the annotation parameters.
     *
     * @param request     The HTTP request
     * @param response    The HTTP response
     * @param rateLimiter The RateLimiter annotation
     * @return true if the request is allowed, false if rate limited
     * @throws IOException if an I/O error occurs
     */
    private boolean applyRateLimit(HttpServletRequest request, HttpServletResponse response, RateLimiter rateLimiter) throws IOException {

        // Create bucket configuration from annotation
        BucketConfiguration bucketConfig = getBucketConfiguration(rateLimiter);

        // Resolve client key based on the annotation's key resolver strategy
        String clientKey = resolveClientKey(request, rateLimiter);
        log.info(clientKey);
        // Get or create bucket for this client
        Bucket bucket = proxyManager.builder().build(clientKey, () -> bucketConfig);

        // Try to consume a token from the bucket
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            // Rate limit exceeded, set error response
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().append("Rate limit exceeded. Try again in ").append(String.valueOf(waitForRefill)).append(" seconds").append(" ").append(getClientIp(request));
            return false;
        }
    }

    private BucketConfiguration getBucketConfiguration(RateLimiter rateLimiter) {
        long tokens = rateLimiter.limit();
        Duration period = Duration.of(rateLimiter.duration(), rateLimiter.unit());
        return BucketConfiguration.builder().addLimit(limit -> limit.capacity(tokens).refillGreedy(tokens, period)).build();
    }

    private String resolveClientKey(HttpServletRequest request, RateLimiter rateLimiter) {
        String prefix = "MEGAFYK" + "rate_limit_" + request.getRequestURI() + ":";

        switch (rateLimiter.keyResolver()) {
            case HEADER:
                String headerValue = request.getHeader(rateLimiter.headerName());
                if (headerValue == null || headerValue.isEmpty()) {
                    // Fallback to IP if header is not present
                    return prefix + getClientIp(request);
                }
                return prefix + headerValue;
            case EXPRESSION:
                if (rateLimiter.keyExpression().isEmpty()) {
                    return prefix + getClientIp(request);
                }
                StandardEvaluationContext context = new StandardEvaluationContext();
                context.setVariable("request", request);
                String value = expressionParser.parseExpression(rateLimiter.keyExpression()).getValue(context, String.class);
                return prefix + (value != null ? value : getClientIp(request));
            default:
                return prefix + getClientIp(request);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
