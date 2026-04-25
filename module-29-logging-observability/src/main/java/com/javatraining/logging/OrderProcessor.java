package com.javatraining.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates all five SLF4J log levels and the {} parameterised placeholder.
 *
 * <p>Key rule: always use {} placeholders - never string concatenation.
 * SLF4J defers {@code toString()} until the message is actually going to be written,
 * so at levels that are disabled the argument is never converted to a String.
 *
 * <pre>
 *   // GOOD - no concatenation at runtime if DEBUG is off
 *   log.debug("Processing {} items for order {}", items.size(), order.id());
 *
 *   // BAD - String concat always happens even when DEBUG is disabled
 *   log.debug("Processing " + items.size() + " items for order " + order.id());
 * </pre>
 */
public class OrderProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessor.class);

    public OrderResult processOrder(Order order) {
        if (order.id() == null) throw new IllegalArgumentException("order id must not be null");

        // TRACE - very fine-grained, usually only in dev/debug mode
        log.trace("Entering processOrder orderId={}", order.id());

        log.info("Processing order id={} item='{}' qty={}",
                order.id(), order.item(), order.quantity());

        if (order.quantity() <= 0) {
            log.warn("Rejected order {}: invalid quantity {}", order.id(), order.quantity());
            return new OrderResult(order.id(), OrderStatus.REJECTED, "Invalid quantity");
        }

        if (order.item() == null || order.item().isBlank()) {
            log.warn("Rejected order {}: item name is blank", order.id());
            return new OrderResult(order.id(), OrderStatus.REJECTED, "Item name required");
        }

        // DEBUG - developer information; off in production but helpful in staging
        log.debug("Inventory check passed for item='{}'", order.item());
        log.debug("Persisting order {} to repository", order.id());

        log.info("Order {} accepted successfully", order.id());
        return new OrderResult(order.id(), OrderStatus.ACCEPTED, "Order accepted");
    }

    /**
     * Demonstrates logging an exception - pass the Throwable as the <em>last</em>
     * argument with no placeholder; SLF4J will append the full stack trace.
     *
     * <pre>
     *   log.error("Failed to process order {}", orderId, exception);
     *              ───────────────────────────  ───────  ─────────
     *              message template             arg      Throwable (no {} needed)
     * </pre>
     */
    public OrderResult processOrderSafely(Order order) {
        try {
            return processOrder(order);
        } catch (Exception e) {
            // ERROR - something went wrong and needs attention; always log the exception
            log.error("Unexpected failure processing order {}: {}", order.id(), e.getMessage(), e);
            return new OrderResult(order.id(), OrderStatus.REJECTED, "Internal error");
        }
    }
}
