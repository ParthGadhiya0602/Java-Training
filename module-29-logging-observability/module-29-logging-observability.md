---
title: "Module 29 — Logging & Observability"
nav_order: 29
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-29-logging-observability/src){: .btn .btn-outline }

# Module 29 — Logging & Observability

SLF4J provides a vendor-neutral logging API; Logback is the reference implementation.
Micrometer provides a vendor-neutral metrics API for counters, timers, and gauges.

---

## SLF4J Architecture

SLF4J (Simple Logging Facade for Java) is a **facade** — your application code calls
the SLF4J API, and a binding wired on the classpath routes calls to the real
implementation.  Swapping implementations (e.g. Logback → Log4j2) requires no
source changes.

```
┌──────────────────────────────────────────────────────────────────┐
│  APPLICATION CODE                                                │
│                                                                  │
│    import org.slf4j.Logger;                                      │
│    import org.slf4j.LoggerFactory;                               │
│                                                                  │
│    Logger log = LoggerFactory.getLogger(MyClass.class);          │
│    log.info("Processing order {}", order.id());                  │
└──────────────────────┬───────────────────────────────────────────┘
                       │  calls SLF4J API (slf4j-api.jar)
                       ▼
┌──────────────────────────────────────────────────────────────────┐
│  SLF4J API   (org.slf4j:slf4j-api)                               │
│  Interface layer — Logger, LoggerFactory, MDC, Marker            │
└──────────────────────┬───────────────────────────────────────────┘
                       │  binding (chosen at runtime via classpath)
          ┌────────────┼────────────────────┐
          ▼            ▼                    ▼
   ┌────────────┐  ┌──────────┐   ┌──────────────────┐
   │  Logback   │  │  Log4j2  │   │  java.util.logging│
   │ (default / │  │(enterprise│   │   (JDK built-in)  │
   │ recommended│  │  / async) │   │                  │
   └────────────┘  └──────────┘   └──────────────────┘
   Only ONE binding should be on the classpath.
   Multiple bindings → SLF4J prints a warning and picks one.
```

---

## Log Level Hierarchy

Levels form a strict hierarchy.  Setting a logger to a level **silences everything
below it**.

```
┌──────────────────────────────────────────────────────────────────────┐
│  Level hierarchy  (lowest verbosity → highest verbosity)             │
│                                                                      │
│  OFF                                                                 │
│   │                                                                  │
│   ▼                                                                  │
│  ERROR  ← System is broken.  Needs immediate attention.              │
│           log.error("Payment gateway timeout for order {}", id, ex)  │
│   │                                                                  │
│   ▼                                                                  │
│  WARN   ← Something unexpected, but the system continues.            │
│           log.warn("Retrying request {}, attempt {}", url, n)        │
│   │                                                                  │
│   ▼                                                                  │
│  INFO   ← Normal lifecycle events worth recording.                   │
│           log.info("Order {} accepted, dispatching to fulfilment",id)│
│   │                                                                  │
│   ▼                                                                  │
│  DEBUG  ← Developer information; off in production by default.       │
│           log.debug("Cache miss for key {}", cacheKey)               │
│   │                                                                  │
│   ▼                                                                  │
│  TRACE  ← Very fine-grained; performance-sensitive paths only.       │
│           log.trace("Entering parseHeader, bytes={}", buf.limit())   │
│   │                                                                  │
│   ▼                                                                  │
│  ALL                                                                 │
└──────────────────────────────────────────────────────────────────────┘

  Logger set to INFO → emits INFO, WARN, ERROR (DEBUG and TRACE are silent)
  Logger set to DEBUG → emits DEBUG, INFO, WARN, ERROR (TRACE is silent)
```

---

## Parameterised Logging

Always use `{}` placeholders — **never string concatenation**.

```java
// GOOD — toString() is NOT called when DEBUG is disabled
log.debug("Processing {} items for order {}", items.size(), order.id());

// BAD — String concatenation always happens, even when DEBUG is off
log.debug("Processing " + items.size() + " items for order " + order.id());
```

Pass a `Throwable` as the **last argument** with no placeholder — SLF4J appends
the full stack trace automatically:

```java
log.error("Failed to process order {}: {}", order.id(), e.getMessage(), e);
//                                                                        ↑
//                                         Throwable — no {} needed here
```

---

## Logback Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  LOGGER  (ch.qos.logback.classic.Logger)                            │
│  • One per class (LoggerFactory.getLogger(MyClass.class))           │
│  • Inherits level from parent if not set explicitly                 │
│  • ROOT logger is the top-level parent                              │
│                                                                     │
│  Logger hierarchy mirrors Java package structure:                   │
│    ROOT → com → com.javatraining → com.javatraining.logging        │
└──────────────┬──────────────────────────────────────────────────────┘
               │  dispatches ILoggingEvent to attached appenders
       ┌───────┼───────────────┐
       ▼       ▼               ▼
┌──────────┐ ┌──────────┐ ┌──────────────────────────┐
│ Console  │ │  File /  │ │      Async               │
│ Appender │ │ Rolling  │ │  (wraps another appender; │
│          │ │ Appender │ │   uses a blocking queue)  │
└────┬─────┘ └────┬─────┘ └──────────────────────────┘
     │            │
     ▼            ▼
┌──────────────────────────────────────────────────────────────────┐
│  ENCODER / LAYOUT                                                │
│  Converts ILoggingEvent → bytes                                  │
│                                                                  │
│  PatternLayout conversion words:                                 │
│    %d{HH:mm:ss.SSS}  timestamp                                   │
│    [%thread]         thread name                                 │
│    %-5level          level, left-padded to 5 chars               │
│    %logger{36}       logger name (class), max 36 chars           │
│    %X{requestId}     MDC value for key 'requestId'               │
│    %msg              the formatted log message                   │
│    %n                platform newline                            │
│    %ex               exception + stack trace                     │
└──────────────────────────────────────────────────────────────────┘
```

### logback.xml — Key Elements

```xml
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%X{requestId}] - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Rolling file: rotate daily, keep 30 days, total cap 1 GB -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- All loggers default to INFO -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

    <!-- Override for our own code: lower threshold to DEBUG -->
    <logger name="com.javatraining" level="DEBUG"/>

</configuration>
```

---

## MDC — Mapped Diagnostic Context

MDC is a **thread-local key/value store**.  Values placed in MDC appear automatically
in every log line on that thread — no need to pass them to every method.

```
  HTTP Request Thread
  ─────────────────────────────────────────────────────────────
  1. MDC.put("requestId", "req-abc")
     MDC.put("userId",    "usr-42")

  2. controller.handle()             ← logs: [req-abc] [usr-42] ...
       └─ service.processOrder()     ← logs: [req-abc] [usr-42] ...
            └─ repo.save()           ← logs: [req-abc] [usr-42] ...

  3. MDC.clear()   ← ALWAYS in a finally block
  ─────────────────────────────────────────────────────────────
  Thread pool: if MDC.clear() is skipped, the next request
  on this thread inherits stale userId/requestId.
```

```java
// Pattern: set → work → clear (in finally)
MDC.put("userId", userId);
MDC.put("requestId", requestId);
try {
    log.info("Request started");
    work.run();
    log.info("Request completed");
} finally {
    MDC.clear();   // ← never skip this
}
```

Logback pattern to print MDC values:
```
%d{HH:mm:ss} [%X{requestId}] [%X{userId}] %-5level %logger - %msg%n
```

---

## Micrometer — Metrics Facade

Micrometer is to metrics what SLF4J is to logging: a vendor-neutral facade.
Your application code uses Micrometer's API; at runtime you plug in a registry
that ships data to Prometheus, Datadog, CloudWatch, Graphite, etc.

```
┌─────────────────────────────────────────────────────────────────────┐
│  APPLICATION CODE                                                   │
│                                                                     │
│    Counter.builder("app.requests.total")                            │
│           .tag("service", "order")                                  │
│           .register(registry)                                       │
│           .increment();                                             │
└──────────────────────────┬──────────────────────────────────────────┘
                           │  MeterRegistry (injected)
          ┌────────────────┼─────────────────────────┐
          ▼                ▼                         ▼
  ┌──────────────┐  ┌─────────────────┐  ┌────────────────────┐
  │  Simple      │  │   Prometheus    │  │   Datadog /        │
  │ MeterRegistry│  │  MeterRegistry  │  │   CloudWatch / ... │
  │ (tests/local)│  │ (production std)│  │                    │
  └──────────────┘  └─────────────────┘  └────────────────────┘
```

### The Four Core Meter Types

```
┌─────────────────────┬──────────────────────────────┬──────────────────────────────┐
│  Meter Type         │  Measures                    │  Typical Use                 │
├─────────────────────┼──────────────────────────────┼──────────────────────────────┤
│  Counter            │  Monotonically increasing    │  Requests, errors, events    │
│                     │  count                       │  app.requests.total          │
├─────────────────────┼──────────────────────────────┼──────────────────────────────┤
│  Timer              │  Duration + count            │  HTTP latency, DB query time │
│                     │  (nanosecond precision)      │  app.http.request.duration   │
├─────────────────────┼──────────────────────────────┼──────────────────────────────┤
│  Gauge              │  Current point-in-time value │  Queue depth, heap MB,       │
│                     │  (sampled on scrape)         │  active connections          │
├─────────────────────┼──────────────────────────────┼──────────────────────────────┤
│  DistributionSummary│  Value distribution:         │  Payload sizes, batch counts │
│                     │  count + total + histogram   │  app.payload.bytes           │
└─────────────────────┴──────────────────────────────┴──────────────────────────────┘
```

### Tags — Partitioning Metrics

Tags turn one metric name into a family of time series:

```
  app.requests.total{service="order"}   ← separate time series
  app.requests.total{service="payment"} ← per tag-set
  app.requests.total{service="shipping"}

  Counter.builder("app.requests.total")
         .tag("service", service)   // tag key + value
         .register(registry)
         .increment();
```

### Meter Recipes

```java
// Counter — increment on each event
Counter.builder("app.requests.total")
       .tag("service", "order")
       .description("Total HTTP requests")
       .register(registry)
       .increment();

// Timer — wrap a block; records count + total time + histogram
Timer timer = Timer.builder("app.operation.duration")
                   .tag("operation", "save")
                   .register(registry);

Result r = timer.record(() -> repo.save(entity));   // captures duration

// Gauge — register once; reads live value on each scrape
AtomicInteger active = new AtomicInteger();
Gauge.builder("app.connections.active", active, AtomicInteger::get)
     .description("Active connections")
     .register(registry);

// DistributionSummary — track value distribution
DistributionSummary.builder("app.payload.bytes")
                   .baseUnit("bytes")
                   .register(registry)
                   .record(response.contentLength());
```

---

## Capture Logs in Tests

Use Logback's `ListAppender` to intercept log events in unit tests without
touching the file system or stdout:

```java
// 1. Get the Logback logger for the class under test
ch.qos.logback.classic.Logger logger =
    (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(OrderProcessor.class);

// 2. Attach an in-memory appender
ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
listAppender.start();
logger.addAppender(listAppender);

// 3. Run the code
processor.processOrder(new Order("ORD-001", "Laptop", 1));

// 4. Assert on captured events
boolean hasWarn = listAppender.list.stream()
    .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("ORD-001"));

// 5. Detach in @AfterEach
logger.detachAppender(listAppender);
```

---

## Logging Best Practices

```
  DO                                          DON'T
  ──────────────────────────────────────────  ────────────────────────────────────────
  log.info("Order {} accepted", order.id())   log.info("Order " + id + " accepted")
  Use {} placeholders (lazy toString)         Concatenate strings before logging

  log.error("Failed: {}", msg, exception)     log.error("Failed: " + exception)
  Pass Throwable last (no placeholder)        Lose the stack trace

  MDC.clear() in finally block                Skip MDC.clear() in a thread pool

  log.debug("Cache miss key={}", key)         Sprinkle DEBUG in hot loops
  Use DEBUG for developer diagnostic info     Use INFO for low-value noise

  Set package-level DEBUG in logback.xml      Turn DEBUG on globally
  <logger name="com.myapp" level="DEBUG"/>    <root level="DEBUG"/>
```

---

## Module 29 — What Was Built

```
  module-29-logging-observability/
  ├── pom.xml                          slf4j-api + logback-classic + micrometer-core
  ├── src/main/
  │   ├── java/com/javatraining/logging/
  │   │   ├── Order.java               domain record
  │   │   ├── OrderResult.java         result record
  │   │   ├── OrderStatus.java         enum
  │   │   ├── OrderProcessor.java      demonstrates all 5 log levels + {} placeholders
  │   │   ├── UserService.java         MDC pattern — set/use/clear in finally
  │   │   └── MetricsService.java      Counter, Timer, Gauge, DistributionSummary
  │   └── resources/
  │       └── logback.xml              ConsoleAppender; RollingFileAppender commented out
  └── src/test/
      ├── java/com/javatraining/logging/
      │   ├── LoggingFeaturesTest.java  13 tests — log levels, MDC lifecycle
      │   └── MicrometerMetricsTest.java 15 tests — Counter, Timer, Gauge, Summary
      └── resources/
          └── logback-test.xml         TRACE level; compact pattern for test output
```

Total: **28 tests**, all passing.
