package com.javatraining.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Demonstrates Mapped Diagnostic Context (MDC) - thread-local key/value pairs
 * that are automatically included in every log line emitted from the same thread.
 *
 * <p>MDC is useful in web servers: set {@code userId} and {@code requestId} once at
 * the entry point; every downstream log call on that thread carries those values
 * without passing them explicitly to every method.
 *
 * <pre>
 *   Logback pattern that prints MDC:
 *   %d{HH:mm:ss} [%X{requestId}] [%X{userId}] %-5level %logger - %msg%n
 *                 ──────────────   ─────────────
 *                 %X{key} reads from MDC
 * </pre>
 *
 * <p><strong>Critical:</strong> always call {@link MDC#clear()} in a {@code finally}
 * block.  MDC is thread-local - if threads are pooled (servlet containers, virtual
 * threads), stale values leak into the next request on that thread.
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * Wraps a unit of work with MDC context.
     * All logs emitted by {@code work} (on this thread) automatically include
     * the userId and requestId.
     */
    public void executeRequest(String userId, String requestId, Runnable work) {
        MDC.put("userId", userId);
        MDC.put("requestId", requestId);
        try {
            log.info("Request started");
            work.run();
            log.info("Request completed");
        } catch (Exception e) {
            log.error("Request failed: {}", e.getMessage());
            throw e;
        } finally {
            MDC.clear();   // ← prevent MDC leaking into the next request on this thread
        }
    }

    /**
     * Returns the current MDC value for {@code key}, or {@code null} if not set.
     * Useful in tests to verify MDC was cleared.
     */
    public String getMdcValue(String key) {
        return MDC.get(key);
    }
}
