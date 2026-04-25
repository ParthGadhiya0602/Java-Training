---
title: "Module 54 — Observability"
parent: "Phase 6 — Production & Architecture"
nav_order: 54
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-54-observability/src){: .btn .btn-outline }

# Module 54 — Observability

## What this module covers

Production observability with three pillars: **metrics** (Micrometer Counter, Timer, Gauge),
**traces** (Micrometer Tracing bridged to OpenTelemetry), and **structured logs** (Logback
with MDC traceId/spanId). The Prometheus scrape endpoint exposes metrics in the format
expected by Prometheus + Grafana.

---

## Project structure

```
src/main/java/com/javatraining/observability/
├── ObservabilityApplication.java
└── product/
    ├── Product.java               # JPA entity
    ├── ProductRepository.java
    ├── ProductService.java        # instrumented with Counter, Timer, Gauge
    └── ProductController.java

src/main/resources/
├── application.properties        # actuator exposure + prometheus enable flag
└── logback-spring.xml            # traceId/spanId in log pattern

src/test/java/com/javatraining/observability/
├── product/
│   └── ProductServiceTest.java   # unit tests with SimpleMeterRegistry (3 tests)
└── ActuatorObservabilityTest.java # @SpringBootTest, prometheus endpoint (2 tests)
```

---

## The three Micrometer instruments

```java
public ProductService(ProductRepository productRepository, MeterRegistry meterRegistry) {
    this.lookupCounter = Counter.builder("products.lookups")
            .description("Total number of product lookup calls")
            .register(meterRegistry);

    this.lookupTimer = Timer.builder("products.lookup.duration")
            .description("Time taken to look up a product by id")
            .register(meterRegistry);

    Gauge.builder("products.active", activeCount, AtomicInteger::get)
            .description("Number of products created in this application instance")
            .register(meterRegistry);
}
```

| Instrument | When to use | Prometheus suffix |
|------------|-------------|-------------------|
| `Counter`  | Monotonically increasing value — requests, errors, events | `_total` |
| `Timer`    | Latency and throughput — method duration, request time | `_seconds_{count,sum,max}` |
| `Gauge`    | Point-in-time snapshot — queue depth, active connections | none |

Micrometer converts `.` to `_` and adds instrument-specific suffixes when scraping.
`products.lookups` → `products_lookups_total`;
`products.lookup.duration` → `products_lookup_duration_seconds_count` etc.

### Recording with a Timer

```java
public Optional<Product> findById(Long id) {
    return lookupTimer.record(() -> {
        lookupCounter.increment();
        return productRepository.findById(id);
    });
}
```

`Timer.record(Supplier<T>)` wraps the block and records its duration. Both the counter
and timer capture the same call, which is typical — you need count AND latency.

---

## @Observed — automatic metrics + trace per method

For methods where you want both a Timer and a trace span without writing boilerplate,
annotate with `@Observed` (requires `spring-boot-starter-aop`):

```java
// io.micrometer.observation.annotation.Observed
@Service
@Observed(name = "product.service")
public class ProductService { ... }
```

Spring AOP wraps each public method in a `Observation`, which fires both a Timer metric
(`product.service.seconds`) and a trace span via the configured tracing bridge. The
`name` maps to the metric name prefix and the span name.

`@Observed` is the high-level shortcut; `Counter`/`Timer`/`Gauge` give fine-grained
control when you need custom tags, descriptions, or specific instrumentation points.

---

## Prometheus endpoint

### Configuration

```properties
management.endpoints.web.exposure.include=health,prometheus
management.prometheus.metrics.export.enabled=true
```

`management.prometheus.metrics.export.enabled=true` is required in Spring Boot 3.3 to
activate `PrometheusMetricsExportAutoConfiguration`. Without it, the condition
`@ConditionalOnEnabledMetricsExport` evaluates false and the endpoint is not registered.

### Scrape output (excerpt)

```
# HELP products_lookups_total Total number of product lookup calls
# TYPE products_lookups_total counter
products_lookups_total 3.0

# HELP products_lookup_duration_seconds Time taken to look up a product by id
# TYPE products_lookup_duration_seconds summary
products_lookup_duration_seconds_count 3.0
products_lookup_duration_seconds_sum 0.002341
products_lookup_duration_seconds_max 0.001821

# HELP products_active Number of products created in this application instance
# TYPE products_active gauge
products_active 2.0
```

A Prometheus `scrape_config` points at `/actuator/prometheus`. Grafana queries Prometheus
and plots dashboards from these time series.

---

## Distributed tracing

`micrometer-tracing-bridge-otel` bridges the Micrometer `Tracer` API to the OpenTelemetry SDK.
Every instrumented HTTP request automatically gets a trace ID and span ID:

```
19:15:42.113 INFO  [main] traceId=4d3a2b1c0f8e7d6a spanId=1a2b3c4d product.service - Looking up product id=1
```

### Sampling

```properties
management.tracing.sampling.probability=1.0   # 100% — every request traced (dev/test)
# In production, use 0.1 (10%) or lower to reduce overhead
```

### Exporters

With no exporter configured, spans are created but discarded. Add a dependency to export:

| Exporter | Dependency | Target |
|----------|-----------|--------|
| OTLP/gRPC | `io.opentelemetry:opentelemetry-exporter-otlp` | Jaeger, Grafana Tempo |
| Zipkin    | `io.opentelemetry:opentelemetry-exporter-zipkin` | Zipkin |
| Logging   | `io.opentelemetry:opentelemetry-exporter-logging` | Console (dev) |

Configure the endpoint: `management.otlp.tracing.endpoint=http://jaeger:4317`

---

## Structured logging with MDC

`logback-spring.xml` injects Micrometer Tracing's MDC keys into every log line:

```xml
<pattern>%d{HH:mm:ss.SSS} %-5level [%thread] traceId=%X{traceId:-} spanId=%X{spanId:-} %logger{36} - %msg%n</pattern>
```

`%X{traceId:-}` prints the MDC value for `traceId`, or empty string if absent (unit tests).
When deployed with a distributed tracing backend, this lets you jump from a log line
directly to the corresponding trace by searching for `traceId=<value>` in your log
aggregator (Loki, Elasticsearch, Splunk).

---

## Testing

### Unit tests — `SimpleMeterRegistry`

```java
private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
private final ProductService productService = new ProductService(mock(ProductRepository.class), meterRegistry);

@Test
void lookup_counter_increments_on_each_findById_call() {
    productService.findById(1L);
    productService.findById(2L);
    assertThat(meterRegistry.counter("products.lookups").count()).isEqualTo(2.0);
}

@Test
void lookup_timer_records_one_observation_per_findById_call() {
    productService.findById(1L);
    assertThat(meterRegistry.timer("products.lookup.duration").count()).isEqualTo(1L);
}

@Test
void active_gauge_reflects_number_of_products_saved() {
    productService.save(new Product(...));
    productService.save(new Product(...));
    assertThat(meterRegistry.get("products.active").gauge().value()).isEqualTo(2.0);
}
```

`SimpleMeterRegistry` keeps all meter state in memory synchronously.
`meterRegistry.counter(name)` returns the same `Counter` instance that was registered
with the same name — no Spring context needed.

### Integration tests — Prometheus endpoint

```java
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorObservabilityTest {

    @Test
    void prometheus_endpoint_exposes_custom_counter_and_timer_after_service_use() throws Exception {
        productService.findById(99L);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("products_lookups_total")))
                .andExpect(content().string(containsString("products_lookup_duration_seconds")));
    }
}
```

Calling `productService.findById(99L)` before scraping ensures the meters appear in the
registry. Meters are lazily registered on first use; the Prometheus endpoint only includes
meters that have been touched.

---

## Tests

| Class | Type | Count |
|---|---|---|
| `ProductServiceTest` | Unit (`SimpleMeterRegistry`) | 3 |
| `ActuatorObservabilityTest` | `@SpringBootTest` | 2 |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **5/5 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `SimpleMeterRegistry` in unit tests | No Spring context — fast, deterministic; same interface as production `MeterRegistry` |
| `management.prometheus.metrics.export.enabled=true` required | Spring Boot 3.3's `@ConditionalOnEnabledMetricsExport` defaults the Prometheus exporter to off; the property must be explicit |
| `Timer.record(Supplier<T>)` over `Timer.wrap()` | Captures both counter increment and timing in one closure, keeping them co-located |
| `AtomicInteger` for Gauge vs `productRepository::count` | Avoids a DB call per scrape; appropriate for an instance-level count. Use `productRepository::count` when the gauge must reflect real DB state |
| `management.tracing.sampling.probability=1.0` | 100% sampling for dev/test; reduce to 0.1–0.01 in production to limit overhead and storage |
| MDC with `%X{traceId:-}` in Logback | `-` fallback makes the pattern safe when no trace context exists (unit tests, startup) |
{% endraw %}
