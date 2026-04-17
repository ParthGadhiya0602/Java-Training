package com.javatraining.logging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Micrometer metrics.
 *
 * <p>{@link SimpleMeterRegistry} stores all meter state in memory — perfect for
 * unit tests. In production you replace it with a registry bound to Prometheus,
 * Datadog, CloudWatch etc. via a dependency; application code never changes.
 *
 * <pre>
 *   Test setup:
 *     new SimpleMeterRegistry()  →  MetricsService(registry)
 *                                       │
 *                          ┌────────────┼───────────────────┐
 *                          ▼            ▼                   ▼
 *                       Counter      Timer               Gauge
 *                   (requests)  (operation time)  (active connections)
 * </pre>
 */
class MicrometerMetricsTest {

    private SimpleMeterRegistry registry;
    private MetricsService service;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        service  = new MetricsService(registry);
    }

    // ── Counter tests ─────────────────────────────────────────────────────────

    @Test
    void counter_starts_at_one_after_first_record() {
        service.recordRequest("order");
        Counter counter = registry.get("app.requests.total").tag("service", "order").counter();
        assertEquals(1.0, counter.count());
    }

    @Test
    void counter_increments_by_one_per_call() {
        service.recordRequest("order");
        service.recordRequest("order");
        service.recordRequest("order");

        double count = registry.get("app.requests.total").tag("service", "order").counter().count();
        assertEquals(3.0, count);
    }

    @Test
    void counter_accumulates_across_many_calls() {
        for (int i = 0; i < 10; i++) service.recordRequest("payment");

        assertEquals(10.0,
                registry.get("app.requests.total").tag("service", "payment").counter().count());
    }

    @Test
    void counter_increments_by_custom_amount() {
        service.recordRequests("batch", 5.0);
        service.recordRequests("batch", 3.0);

        assertEquals(8.0,
                registry.get("app.requests.total").tag("service", "batch").counter().count());
    }

    @Test
    void tags_partition_counter_into_separate_time_series() {
        service.recordRequest("order");
        service.recordRequest("order");
        service.recordRequest("payment");

        double orderCount   = registry.get("app.requests.total").tag("service", "order").counter().count();
        double paymentCount = registry.get("app.requests.total").tag("service", "payment").counter().count();

        assertEquals(2.0, orderCount,   "order counter should be 2");
        assertEquals(1.0, paymentCount, "payment counter should be 1");
    }

    // ── Timer tests ───────────────────────────────────────────────────────────

    @Test
    void timer_count_equals_number_of_recorded_calls() {
        service.timeVoidOperation("process", () -> {});
        service.timeVoidOperation("process", () -> {});
        service.timeVoidOperation("process", () -> {});

        Timer timer = registry.get("app.operation.duration").tag("operation", "process").timer();
        assertEquals(3, timer.count());
    }

    @Test
    void timer_total_time_is_non_negative_after_recording() {
        service.timeVoidOperation("save", () -> {
            long x = 0;
            for (int i = 0; i < 1000; i++) x += i;
        });

        Timer timer = registry.get("app.operation.duration").tag("operation", "save").timer();
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.NANOSECONDS) >= 0,
                   "Total time should be non-negative");
    }

    @Test
    void timer_returns_supplier_result() {
        String result = service.timeOperation("compute", () -> "hello");

        assertEquals("hello", result, "timeOperation should return the supplier's value");
    }

    @Test
    void timer_tags_create_separate_time_series() {
        service.timeVoidOperation("read",  () -> {});
        service.timeVoidOperation("write", () -> {});
        service.timeVoidOperation("write", () -> {});

        long readCount  = registry.get("app.operation.duration").tag("operation", "read").timer().count();
        long writeCount = registry.get("app.operation.duration").tag("operation", "write").timer().count();

        assertEquals(1, readCount);
        assertEquals(2, writeCount);
    }

    // ── Gauge tests ───────────────────────────────────────────────────────────

    @Test
    void gauge_starts_at_zero() {
        Gauge gauge = registry.get("app.connections.active").gauge();
        assertEquals(0.0, gauge.value());
    }

    @Test
    void gauge_increases_on_connect() {
        service.connect();
        service.connect();

        assertEquals(2.0, registry.get("app.connections.active").gauge().value());
        assertEquals(2,   service.getActiveConnections());
    }

    @Test
    void gauge_decreases_on_disconnect() {
        service.connect();
        service.connect();
        service.connect();
        service.disconnect();

        assertEquals(2.0, registry.get("app.connections.active").gauge().value());
    }

    @Test
    void gauge_reflects_live_state_without_re_registration() {
        Gauge gauge = registry.get("app.connections.active").gauge();

        service.connect();
        assertEquals(1.0, gauge.value(), "Gauge must see live state after connect");

        service.disconnect();
        assertEquals(0.0, gauge.value(), "Gauge must see live state after disconnect");
    }

    // ── DistributionSummary tests ─────────────────────────────────────────────

    @Test
    void distribution_summary_tracks_count_and_total() {
        service.recordPayloadSize(100);
        service.recordPayloadSize(200);
        service.recordPayloadSize(300);

        var summary = registry.get("app.payload.bytes").summary();
        assertEquals(3,     summary.count());
        assertEquals(600.0, summary.totalAmount());
    }

    @Test
    void distribution_summary_mean_equals_total_over_count() {
        service.recordPayloadSize(400);
        service.recordPayloadSize(600);

        var summary = registry.get("app.payload.bytes").summary();
        assertEquals(500.0, summary.mean(), 0.001);
    }
}
