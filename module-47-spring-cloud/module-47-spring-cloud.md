# Module 47 — Spring Cloud

## Overview

Spring Cloud is an umbrella of libraries that solve the common cross-cutting concerns
that arise when running Java services in a distributed system: where to find other
services (discovery), where to get configuration (Config Server), how to route traffic
(Gateway), how to tolerate partial failures (Circuit Breaker), and how to trace a
request as it crosses service boundaries (distributed tracing).

---

## 1. Spring Cloud BOM

Spring Cloud releases are independent of Spring Boot and are managed through their own BOM:

```xml
<properties>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Spring Cloud `2023.0.x` is compatible with Spring Boot `3.3.x`. Individual starters
are then added without an explicit version — the BOM manages them.

---

## 2. Config Server

Externalises configuration into a central server. All services fetch their properties
at startup (and optionally at runtime via `/actuator/refresh`).

### Server setup

```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication { ... }
```

```properties
# application.properties for the Config Server itself
server.port=8888
spring.cloud.config.server.git.uri=https://github.com/example/config-repo
spring.cloud.config.server.git.default-label=main
```

Alternatively, use a local filesystem back-end for development:

```properties
spring.cloud.config.server.native.search-locations=classpath:/config
spring.profiles.active=native
```

### Client setup

Any service that wants centralised config adds:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

```properties
# bootstrap.properties (loaded before application.properties)
spring.application.name=order-service
spring.config.import=optional:configserver:http://localhost:8888
```

The Config Server serves `{application-name}/{profile}` — so `order-service/production`
maps to `order-service-production.properties` in the git repo.

---

## 3. Eureka — Service Discovery

Eureka lets services find each other by name instead of hard-coded URLs.

### Server

```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication { ... }
```

```properties
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### Client

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```properties
spring.application.name=order-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

`@LoadBalanced` RestClient resolves service names via Eureka:

```java
@Bean
@LoadBalanced
RestClient.Builder loadBalancedBuilder() {
    return RestClient.builder();
}

// Later: restClient.get().uri("http://inventory-service/inventory/{id}", id)
// "inventory-service" is resolved to an actual host:port by the load balancer
```

---

## 4. API Gateway

Spring Cloud Gateway is a reactive edge service that routes requests to downstream services,
applies filters (auth, rate limiting, header rewriting), and integrates with Eureka for
load balancing.

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

Routes are declared in `application.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service          # lb:// means resolve via Eureka
          predicates:
            - Path=/orders/**
          filters:
            - StripPrefix=0
            - AddRequestHeader=X-Gateway-Source, api-gateway

        - id: inventory-service
          uri: lb://inventory-service
          predicates:
            - Path=/inventory/**
          filters:
            - CircuitBreaker=name=inventory,fallbackUri=/fallback/inventory
```

**Key concepts:**
- `Predicate` — matches an incoming request (path, method, header, query param)
- `Filter` — mutates the request or response (add header, rate limit, circuit break)
- `lb://` prefix — routes through the load balancer (requires Eureka or similar)
- `StripPrefix=N` — removes N path segments before forwarding

Gateway runs on Spring WebFlux and requires a separate application (cannot share a JVM with a Spring MVC service).

---

## 5. Circuit Breaker — Resilience4j

A circuit breaker wraps a call to a remote service and tracks failures.
When the failure rate exceeds a threshold the circuit **opens**: subsequent calls
return a fallback immediately without reaching the downstream service, letting it recover.
After a wait period one **probe** call is allowed (HALF_OPEN); success closes the circuit,
failure keeps it open.

```
     Calls
       │
  ┌────▼──────────────────────────────┐
  │  CLOSED                           │  failure rate < threshold
  │  All calls pass through           │
  └────────────────┬──────────────────┘
                   │ failure rate ≥ threshold
                   ▼
  ┌────────────────────────────────────┐
  │  OPEN                              │  all calls → fallback immediately
  │  No calls reach downstream         │
  └────────────────┬───────────────────┘
                   │ after wait-duration-in-open-state
                   ▼
  ┌────────────────────────────────────┐
  │  HALF_OPEN                         │  one probe call allowed
  │  permitted-number-of-calls         │  success → CLOSED
  └────────────────────────────────────┘  failure → OPEN
```

### Annotation-based (Resilience4j)

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```java
@Service
public class InventoryService {

    @CircuitBreaker(name = "inventory", fallbackMethod = "availabilityFallback")
    public boolean checkAvailability(Long productId, int quantity) {
        return inventoryClient.checkAvailability(productId, quantity);
    }

    boolean availabilityFallback(Long productId, int quantity, Exception e) {
        log.warn("CB fallback: {}", e.getMessage());
        return false;   // degrade gracefully
    }
}
```

Configuration:

```properties
resilience4j.circuitbreaker.instances.inventory.sliding-window-size=10
resilience4j.circuitbreaker.instances.inventory.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.inventory.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.inventory.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.inventory.permitted-number-of-calls-in-half-open-state=3
```

### Spring Cloud Circuit Breaker abstraction

For provider-agnostic code, use `CircuitBreakerFactory`:

```java
@Autowired CircuitBreakerFactory circuitBreakerFactory;

public boolean checkAvailability(Long productId, int quantity) {
    return circuitBreakerFactory
            .create("inventory")
            .run(
                () -> inventoryClient.checkAvailability(productId, quantity),
                throwable -> false  // fallback
            );
}
```

`CircuitBreakerFactory` is auto-configured by `spring-cloud-starter-circuitbreaker-resilience4j`.
Swapping to a different implementation (e.g. Sentinel) only requires a dependency change.

---

## 6. Retry

Retry is complementary to Circuit Breaker: it handles transient failures by re-attempting
the call before giving up, while the Circuit Breaker handles sustained failures by stopping
calls entirely.

```java
@Retry(name = "inventory", fallbackMethod = "reserveFallback")
public boolean reserve(Long productId, int quantity) {
    return inventoryClient.reserve(productId, quantity);
}
```

```properties
resilience4j.retry.instances.inventory.max-attempts=3
resilience4j.retry.instances.inventory.wait-duration=500ms
resilience4j.retry.instances.inventory.enable-exponential-backoff=true
resilience4j.retry.instances.inventory.exponential-backoff-multiplier=2
```

With exponential backoff and `max-attempts=3`, waits are: 500ms, 1000ms — then fallback.

**Stacking Retry inside Circuit Breaker:**

```java
@CircuitBreaker(name = "inventory", fallbackMethod = "fallback")
@Retry(name = "inventory")
public boolean reserve(Long productId, int quantity) { ... }
```

The Retry aspect is innermost: it retries the HTTP call. If all retries fail, the
exception propagates to the Circuit Breaker which counts the entire retry batch as one
failure. This is the recommended production configuration.

---

## 7. Distributed Tracing

Distributed tracing follows a request across service boundaries by attaching a `traceId`
(constant for the whole request) and `spanId` (unique per service hop) to every log line
and HTTP header.

### Micrometer Tracing + Brave

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```properties
management.tracing.sampling.probability=1.0     # 100% in dev; 0.1 (10%) in prod
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
```

Spring Boot auto-configures Brave and injects `traceId`/`spanId` into MDC —
every `log.info(...)` call automatically includes them.

### `@Observed` — custom spans

```java
@Service
@Observed(name = "inventory.service")
public class InventoryService { ... }
```

Or on individual methods:

```java
@Observed(name = "inventory.reserve",
          contextualName = "reserve-inventory",
          lowCardinalityKeyValues = {"service", "inventory"})
public boolean reserve(Long productId, int quantity) { ... }
```

Requires `spring-boot-starter-aop` and `spring-boot-starter-actuator` on the classpath.
Spring Boot auto-configures the `ObservedAspect` that intercepts `@Observed` and creates spans.

---

## 8. Testing Circuit Breaker and Retry

Resilience4j state is held in `CircuitBreakerRegistry` and `RetryRegistry` beans.
Tests use tight thresholds (`src/test/resources/application.properties`) and reset
the circuit breaker state between test methods.

```java
@SpringBootTest
class CircuitBreakerTest {

    @Autowired InventoryService inventoryService;
    @Autowired CircuitBreakerRegistry circuitBreakerRegistry;
    @MockBean InventoryClient inventoryClient;

    @BeforeEach
    void reset() {
        circuitBreakerRegistry.circuitBreaker("inventory").reset();
    }

    @Test
    void circuit_opens_after_failure_threshold() {
        when(inventoryClient.checkAvailability(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("unavailable"));

        inventoryService.checkAvailability(1L, 1);
        inventoryService.checkAvailability(1L, 1);

        assertThat(circuitBreakerRegistry.circuitBreaker("inventory").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void open_circuit_returns_fallback_without_reaching_client() {
        when(inventoryClient.checkAvailability(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("unavailable"));

        inventoryService.checkAvailability(1L, 1);
        inventoryService.checkAvailability(1L, 1); // opens

        boolean result = inventoryService.checkAvailability(1L, 1); // short-circuited

        assertThat(result).isFalse();
        verify(inventoryClient, times(2)).checkAvailability(anyLong(), anyInt());
    }

    @Test
    void circuit_closes_after_successful_probe_in_half_open() throws InterruptedException {
        when(inventoryClient.checkAvailability(anyLong(), anyInt()))
                .thenThrow(new RuntimeException())
                .thenThrow(new RuntimeException())
                .thenReturn(true);

        inventoryService.checkAvailability(1L, 1);
        inventoryService.checkAvailability(1L, 1);

        Thread.sleep(150); // > wait-duration-in-open-state=100ms

        inventoryService.checkAvailability(1L, 1); // probe → HALF_OPEN → CLOSED

        assertThat(circuitBreakerRegistry.circuitBreaker("inventory").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
```

**Test configuration** (`src/test/resources/application.properties`):

```properties
resilience4j.circuitbreaker.instances.inventory.sliding-window-size=2
resilience4j.circuitbreaker.instances.inventory.minimum-number-of-calls=2
resilience4j.circuitbreaker.instances.inventory.failure-rate-threshold=100
resilience4j.circuitbreaker.instances.inventory.wait-duration-in-open-state=100ms
resilience4j.circuitbreaker.instances.inventory.permitted-number-of-calls-in-half-open-state=1

resilience4j.retry.instances.inventory.max-attempts=3
resilience4j.retry.instances.inventory.wait-duration=10ms
resilience4j.retry.instances.inventory.enable-exponential-backoff=false
```

---

## Key takeaways

- Spring Cloud BOM `2023.0.x` manages all Spring Cloud artifact versions alongside
  Spring Boot `3.3.x` — import it in `<dependencyManagement>` and add starters without versions
- Config Server externalises configuration into a git repo (or local filesystem);
  clients fetch properties at startup via `spring.config.import=configserver:`
- Eureka enables service discovery: services register by name and clients resolve
  names to host:port at call time; `@LoadBalanced` RestClient handles the resolution
- Spring Cloud Gateway is a reactive edge service (requires WebFlux); routes are
  declared as predicate + filter chains, with `lb://` URIs for Eureka-based load balancing
- Circuit Breaker (`@CircuitBreaker`) stops cascading failures by short-circuiting
  calls to unhealthy services and returning a fallback; the three states are
  CLOSED → OPEN → HALF_OPEN → CLOSED
- Retry (`@Retry`) handles transient failures; stacked inside a Circuit Breaker,
  each retry batch counts as a single failure observation for the CB
- Test with tight thresholds in `src/test/resources/application.properties` and reset
  `CircuitBreakerRegistry` in `@BeforeEach` to keep tests independent
