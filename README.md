# RS4M - Resilience Service for Microservices

[![Maven Central](https://img.shields.io/maven-central/v/com.rs4m/rs4m.svg)](https://search.maven.org/artifact/com.rs4m/rs4m)
[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.0.4-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

RS4M is a comprehensive resilience library for Spring Boot applications that provides advanced rate limiting capabilities with hot-reload configuration, rule engine integration, and distributed rate limiting support using Redis.

## 🚀 Features

### 🎯 Core Features
- **Annotation-Based Rate Limiting**: Simple `@RateLimiter` annotation for RESTful APIs
- **Hot-Reload Configuration**: Dynamic rate limit configuration updates without restart
- **Distributed Rate Limiting**: Redis-backed distributed rate limiting using Bucket4j
- **Rule Engine Integration**: Pluggable rule engine interface for complex rate limiting logic
- **Multiple Key Resolution Strategies**: IP-based, Header-based, and SpEL expression-based client identification
- **Event-Driven Architecture**: Observer pattern implementation for configuration changes

### 🔧 Advanced Capabilities
- **Flexible Rate Limit Profiles**: YAML-based configuration with multiple bandwidth profiles
- **Real-time Metrics**: Rate limit headers and monitoring support
- **Graceful Fallbacks**: Error handling with fallback strategies
- **Spring Boot Auto-Configuration**: Zero-configuration setup for common use cases

## 📦 Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.rs4m</groupId>
    <artifactId>rs4m</artifactId>
    <version>0.0.1</version>
</dependency>
```

For Redis support and spring boot compatibility, also add:
```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-data-22</artifactId>
    <version>3.16.3</version>
</dependency>
```

## 🏗️ Quick Start

### 1. Simple Rate Limiting

Apply rate limiting to your REST controllers:

```java
@RestController
@RequestMapping("/rs4m/dummy")
@Slf4j
public class DummyController {

    private final Map<Long, DummyItem> itemsDb = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    // Get all items
    @GetMapping
    // define bucket name + bucket rate limit manager + rule engine manager for key resolver bucket
    @RateLimiter(value="dummy_bucket", rateLimitManager = "defaultRateLimitManager", ruleEngineManager = "defaultRuleEngineManager")
    public List<DummyItem> getAllItems() {
        log.info("Fetching all dummy items");
        return new ArrayList<>(itemsDb.values());
    }
}
```

### 3. Configuration
Configure redis RS4M in your `application.properties`:
```properties
rs4m.redisson.node-addresses=localhost:6379,localhost:6380,localhost:6381
rs4m.redisson.password=
rs4m.redisson.timeout=1000

```
Configure rate limit each bucket in your `application.yml`:

```yaml
r4sm:
  rate:
    default:
      buckets:
        dummy_bucket: # bucket name
          enable: true
          version: 0.1
          bandwidths: # bucket configuration
            burst_limit:
              id: burst_limit
              limit: 5
              duration: 1
              timeUnit: MINUTES
            biz_limit:
              id: biz_limit
              limit: 1
              duration: 1
              timeUnit: MINUTES
        foo_bucket: # another bucket name
          enable: true
          version: 0.1
          bandwidths:
            burst_limit:
              id: burst_limit
              limit: 10
              duration: 1
```

### Hot-Reload Configuration

Update rate limit configurations at runtime:

```bash
curl -X POST http://localhost:8080/rs4m/actuator/rate-limit/buckets \
  -H "Content-Type: application/json" \
  -d '{
    "dummy_bucket": {
      "enable": true,
      "version": "0.2",
      "bandwidths": {
        "biz_limit": {
          "id": "biz_limit",
          "limit": 1,
          "duration": 1,
          "timeUnit": "MINUTES"
        }
      }
    }
  }'
```

## 🏛️ Architecture

RS4M follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    @RateLimiter Annotation                  │
├─────────────────────────────────────────────────────────────┤
│                   RateLimiterFilter                         │
├─────────────────────────────────────────────────────────────┤
│  RateLimitManager  │  RuleEngineManager  │  EventManager    │
├─────────────────────────────────────────────────────────────┤
│                      Bucket4j + Redis                       │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **`@RateLimiter`**: Annotation for declarative rate limiting
- **`RateLimiterFilter`**: Servlet filter that intercepts and applies rate limiting
- **`RateLimitManager`**: Manages bucket creation and retrieval
- **`RuleEngineManager`**: Interface for pluggable rule engines
- **`EventManager`**: Handles configuration change events
- **`BucketProfile`**: Configuration model for rate limit profiles

## 🔧 Configuration Reference

### Rate Limiter Properties

| Property | Default | Description |
|----------|---------|-------------|
| `rs4m.rate-limiter.enable` | `true` | Enable/disable rate limiting |
| `rs4m.rate-limiter.default-limit` | `20` | Default request limit |
| `rs4m.rate-limiter.default-duration` | `1` | Default time window duration |
| `rs4m.rate-limiter.default-time-unit` | `HOURS` | Default time unit |

### Annotation Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `value` | Required | Unique identifier for the rate limiter |
| `limit` | `20` | Maximum requests allowed |
| `duration` | `1` | Time window duration |
| `unit` | `MINUTES` | Time unit (SECONDS, MINUTES, HOURS, DAYS) |
| `keyResolver` | `IP` | Key resolution strategy |
| `headerName` | `X-API-KEY` | Header name for HEADER strategy |
| `keyExpression` | `""` | SpEL expression for EXPRESSION strategy |
| `rateLimitManager` | `defaultRateLimitManager` | Bean name of rate limit manager |
| `ruleEngineManager` | `""` | Bean name of rule engine manager |

## 🚦 Rate Limit Responses

When rate limit is exceeded, RS4M returns:

```http
HTTP/1.1 429 Too Many Requests
X-Rate-Limit-Retry-After-Seconds: 60
Content-Type: text/plain

Rate limit exceeded. Try again in 60 seconds 192.168.1.100
```

Successful requests include:
```http
HTTP/1.1 200 OK
X-Rate-Limit-Remaining: 19
```

## 🔌 Extensibility

### Custom Rate Limit Manager

```java
@Component("customRateLimitManager")
public class CustomRateLimitManager implements RateLimitManager {
    
    @Override
    public Bucket getBucket(String clientKey, RateLimiter rateLimiter) {
        // Custom bucket creation logic
        return bucketBuilder.build();
    }
}
```

### Custom Rule Engine

```java
public class DroolsRuleEngine implements RuleEngine {
    
    @Override
    public void loadRules(RuleSource ruleSource) throws RuleEngineException {
        // Load Drools rules
    }

    @Override
    public void fireRules(Object fact) throws RuleEngineException {
        // Execute rules
    }

    @Override
    public <T> T getResult(Class<T> resultType) throws RuleEngineException {
        // Return result
    }
}
```

## 📊 Monitoring & Metrics

RS4M integrates with Spring Boot Actuator for monitoring:

- Rate limit hit/miss metrics
- Bucket configuration status
- Rule engine execution metrics
- Redis connection health

## 🤝 Contributing

We welcome contributions!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the Apache License 2.0.

## 🙏 Acknowledgments

- [Bucket4j](https://github.com/bucket4j/bucket4j) - Java rate limiting library
- [Redisson](https://github.com/redisson/redisson) - Redis Java client
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework

**Made with ❤️ for the community**

