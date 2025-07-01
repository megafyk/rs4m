package com.rs4m.filter;

import com.rs4m.annotation.RateLimiter;
import com.rs4m.observer.RateLimitManager;
import com.rs4m.rule.RuleEngine;
import com.rs4m.rule.RuleEngineManager;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * Rate limiter filter for HTTP requests based on the @RateLimiter annotation.
 */
@Slf4j
@Component
@AllArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {
    private final RequestMappingHandlerMapping handlerMapping;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ApplicationContext applicationContext;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            HandlerExecutionChain handler = handlerMapping.getHandler(request);
            if (handler == null) {
                filterChain.doFilter(request, response);
                return;
            }
            HandlerMethod handlerMethod = (HandlerMethod) handler.getHandler();
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
        // get bean rule engine by bean name
        RuleEngineManager ruleEngineManager = null;
        if (!rateLimiter.ruleEngineManager().isEmpty()) {
            ruleEngineManager = applicationContext.getBean(rateLimiter.ruleEngineManager(), RuleEngineManager.class);
        }

        // Resolve client key based on the annotation's key resolver strategy
        String clientKey = resolveClientKey(request, rateLimiter, ruleEngineManager);
        if (clientKey.isEmpty()) {
            log.warn("Client key is null or empty for request: {}", request.getRequestURI());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.getWriter().append("Internal server error: Client key is null or empty");
            return false;
        }

        // get bean rate limit by bean name
        RateLimitManager rateLimitManager = applicationContext.getBean(rateLimiter.rateLimitManager(), RateLimitManager.class);

        // Get or create bucket for this client
        Bucket bucket = rateLimitManager.getBucket(clientKey, rateLimiter);

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


    private String resolveClientKey(HttpServletRequest request, RateLimiter rateLimiter, RuleEngineManager ruleEngineManager) {
        String prefix = "rs4m_rl_" + request.getRequestURI() + ":";

        // base by rule engine
        if (ruleEngineManager != null) {
            RuleEngine ruleEngine = ruleEngineManager.getEngine(rateLimiter.ruleEngineManager());
            if (ruleEngine != null) {
                try {
                    ruleEngine.fireRules(request);
                    String abc = ruleEngine.getResult(String.class);
                    return prefix + abc;
                } catch (Exception e) {
                    log.error("Error firing rules in RuleEngine: {}", rateLimiter.ruleEngineManager(), e);
                    return "";
                }
            }
        }

        // base by annotation configuration
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
