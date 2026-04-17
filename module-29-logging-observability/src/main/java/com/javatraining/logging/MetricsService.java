package com.javatraining.logging;

import io.micrometer.core.instrument.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Demonstrates the four core Micrometer meter types:
 *
 * <pre>
 *  ┌─────────────────────┬──────────────────────────────────────────────────────┐
 *  │  Meter Type         │  Use-case                                            │
 *  ├─────────────────────┼──────────────────────────────────────────────────────┤
 *  │  Counter            │  Monotonically increasing count: requests, errors    │
 *  │  Timer              │  Duration + count: HTTP latency, DB query time       │
 *  │  Gauge              │  Current point-in-time value: queue depth, heap MB   │
 *  │  DistributionSummary│  Value distribution: payload sizes, batch counts     │
 *  └─────────────────────┴──────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Micrometer is the metrics facade (like SLF4J is the logging facade).
 * In production you swap {@link io.micrometer.core.instrument.simple.SimpleMeterRegistry}
 * for Prometheus, Datadog, CloudWatch etc. — application code never changes.
 */
public class MetricsService {

    // Micrometer registers meters lazily; hold the registry to create meters on demand.
    private final MeterRegistry registry;

    // Gauge wraps a live value — it samples the object on each scrape, not at record time.
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;

        // Register the gauge once at construction — it holds a reference to activeConnections
        // and reads its current value each time the registry is scraped.
        Gauge.builder("app.connections.active", activeConnections, AtomicInteger::get)
             .description("Number of currently open connections")
             .register(registry);
    }

    // ── Counter ──────────────────────────────────────────────────────────────

    /**
     * Increment the request counter for a given service.
     * Tags partition a single metric name: one time-series per (name, tag-set).
     */
    public void recordRequest(String service) {
        Counter.builder("app.requests.total")
               .description("Total number of requests")
               .tag("service", service)
               .register(registry)
               .increment();
    }

    /** Increment by a custom amount (e.g. batch size). */
    public void recordRequests(String service, double count) {
        Counter.builder("app.requests.total")
               .tag("service", service)
               .register(registry)
               .increment(count);
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    /**
     * Wrap an operation with a timer — records both count and total duration.
     * The Timer uses {@code System.nanoTime()} internally.
     */
    public <T> T timeOperation(String name, Supplier<T> operation) {
        return Timer.builder("app.operation.duration")
                    .description("Duration of named operations")
                    .tag("operation", name)
                    .register(registry)
                    .record(operation);
    }

    /** Void variant for convenience. */
    public void timeVoidOperation(String name, Runnable operation) {
        Timer.builder("app.operation.duration")
             .tag("operation", name)
             .register(registry)
             .record(operation);
    }

    // ── Gauge ─────────────────────────────────────────────────────────────────

    /** Increase the active connection count by one. */
    public void connect() {
        activeConnections.incrementAndGet();
    }

    /** Decrease the active connection count by one. */
    public void disconnect() {
        activeConnections.decrementAndGet();
    }

    /** Snapshot of the current connection count (for assertions in tests). */
    public int getActiveConnections() {
        return activeConnections.get();
    }

    // ── DistributionSummary ───────────────────────────────────────────────────

    /**
     * Records the size of a payload (e.g. HTTP response body bytes).
     * DistributionSummary tracks count, total, and histogram buckets.
     */
    public void recordPayloadSize(long bytes) {
        DistributionSummary.builder("app.payload.bytes")
                           .description("HTTP response payload size")
                           .baseUnit("bytes")
                           .register(registry)
                           .record(bytes);
    }
}
