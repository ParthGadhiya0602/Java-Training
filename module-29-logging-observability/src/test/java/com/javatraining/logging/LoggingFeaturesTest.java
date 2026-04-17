package com.javatraining.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SLF4J logging patterns, Logback level filtering, and MDC behaviour.
 *
 * <p><strong>How log capture works in tests:</strong>
 * <pre>
 *   Logback Logger (singleton per name)
 *         │
 *         ├── ConsoleAppender  (writes to stdout)
 *         └── ListAppender     (holds events in memory — attached by tests)
 *                  └── listAppender.list  ←  we assert on these
 * </pre>
 *
 * ListAppender is Logback-specific. In production code we only use SLF4J API;
 * tests may reach into the implementation to verify log output.
 */
class LoggingFeaturesTest {

    private ListAppender<ILoggingEvent> appender;
    private ch.qos.logback.classic.Logger processorLog;
    private ch.qos.logback.classic.Logger userServiceLog;

    private OrderProcessor processor;
    private UserService userService;

    @BeforeEach
    void attachListAppender() {
        processor   = new OrderProcessor();
        userService = new UserService();

        // Loggers are singletons — same instance every call for the same name.
        processorLog   = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(OrderProcessor.class);
        userServiceLog = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(UserService.class);

        appender = new ListAppender<>();
        appender.start();
        processorLog.addAppender(appender);
        userServiceLog.addAppender(appender);
    }

    @AfterEach
    void detachAndClearMdc() {
        processorLog.detachAppender(appender);
        userServiceLog.detachAppender(appender);
        MDC.clear();   // defensive — ensures no MDC leaks between tests
    }

    // ── Log level tests ───────────────────────────────────────────────────────

    @Test
    void accepted_order_emits_info_messages() {
        processor.processOrder(new Order("ORD-001", "Laptop", 1));

        assertTrue(hasEvent(Level.INFO, "ORD-001"),
                   "Expected INFO log containing the order ID");
    }

    @Test
    void accepted_order_emits_debug_messages() {
        // logback-test.xml sets root to TRACE, so DEBUG is visible
        processor.processOrder(new Order("ORD-002", "Tablet", 2));

        assertTrue(hasLevel(Level.DEBUG), "Expected at least one DEBUG event");
    }

    @Test
    void zero_quantity_order_emits_warn() {
        processor.processOrder(new Order("ORD-003", "Phone", 0));

        assertTrue(hasEvent(Level.WARN, "ORD-003"),
                   "Expected WARN for invalid quantity");
    }

    @Test
    void blank_item_order_emits_warn() {
        processor.processOrder(new Order("ORD-004", "  ", 1));

        assertTrue(hasEvent(Level.WARN, "ORD-004"),
                   "Expected WARN for blank item name");
    }

    @Test
    void exception_handling_emits_error() {
        // processOrderSafely wraps a bad call and logs ERROR
        processor.processOrderSafely(new Order(null, "Item", 1));

        assertTrue(hasLevel(Level.ERROR), "Expected ERROR when NullPointerException occurs");
    }

    @Test
    void trace_messages_present_because_test_config_enables_trace() {
        processorLog.setLevel(Level.TRACE);   // ensure TRACE is on for this logger
        processor.processOrder(new Order("ORD-006", "Book", 3));

        assertTrue(hasLevel(Level.TRACE), "Expected at least one TRACE event");
        processorLog.setLevel(null);           // reset to inherited level
    }

    @Test
    void message_contains_parameterised_values() {
        processor.processOrder(new Order("ORD-007", "Monitor", 5));

        // Parameterised placeholders {} must be resolved in the formatted message
        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

        assertTrue(messages.stream().anyMatch(m -> m.contains("Monitor")),
                   "Formatted message should contain the item name");
        assertTrue(messages.stream().anyMatch(m -> m.contains("5")),
                   "Formatted message should contain the quantity");
    }

    @Test
    void info_events_have_non_null_formatted_messages() {
        processor.processOrder(new Order("ORD-008", "Keyboard", 2));

        var infoEvents = appender.list.stream()
                .filter(e -> e.getLevel() == Level.INFO)
                .toList();
        assertFalse(infoEvents.isEmpty(), "There should be INFO events");
        infoEvents.forEach(e -> assertNotNull(e.getFormattedMessage()));
    }

    // ── MDC tests ─────────────────────────────────────────────────────────────

    @Test
    void mdc_values_appear_in_log_events() {
        userService.executeRequest("usr-42", "req-abc", () -> {});

        // All events logged inside executeRequest should carry the MDC values.
        assertTrue(appender.list.stream().anyMatch(e -> {
            var mdc = e.getMDCPropertyMap();
            return "usr-42".equals(mdc.get("userId"))
                && "req-abc".equals(mdc.get("requestId"));
        }), "MDC properties should appear on events logged inside executeRequest");
    }

    @Test
    void mdc_is_cleared_after_normal_execution() {
        userService.executeRequest("usr-1", "req-1", () -> {});

        // MDC.clear() in the finally block removes all keys
        assertNull(MDC.get("userId"),    "userId should be cleared from MDC after request");
        assertNull(MDC.get("requestId"), "requestId should be cleared from MDC after request");
    }

    @Test
    void mdc_is_cleared_even_when_work_throws() {
        assertThrows(RuntimeException.class, () ->
                userService.executeRequest("usr-2", "req-2", () -> {
                    throw new RuntimeException("simulated failure");
                }));

        assertNull(MDC.get("userId"),
                   "MDC must be cleared in finally block even when work throws");
    }

    @Test
    void mdc_values_are_thread_local_and_do_not_bleed_between_requests() {
        userService.executeRequest("usr-A", "req-A", () -> {});
        userService.executeRequest("usr-B", "req-B", () -> {});

        // After both requests, MDC should be empty on this thread.
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("requestId"));

        // Confirm events from first request do NOT carry values from the second request.
        var firstRequestEvents = appender.list.stream()
                .filter(e -> "usr-A".equals(e.getMDCPropertyMap().get("userId")))
                .toList();
        assertFalse(firstRequestEvents.isEmpty(), "First request events should exist");
        firstRequestEvents.forEach(e ->
                assertNotEquals("usr-B", e.getMDCPropertyMap().get("userId")));
    }

    @Test
    void log_level_hierarchy_info_suppresses_debug_when_logger_set_to_info() {
        processorLog.setLevel(Level.INFO);    // override logback-test.xml setting

        processor.processOrder(new Order("ORD-013", "Pen", 1));

        assertFalse(hasLevel(Level.DEBUG), "DEBUG should be suppressed at INFO level");
        assertTrue(hasLevel(Level.INFO),   "INFO should still be emitted");

        processorLog.setLevel(null);          // reset
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean hasEvent(Level level, String fragment) {
        return appender.list.stream().anyMatch(e ->
                e.getLevel() == level && e.getFormattedMessage().contains(fragment));
    }

    private boolean hasLevel(Level level) {
        return appender.list.stream().anyMatch(e -> e.getLevel() == level);
    }
}
