package com.javatraining.springcloud.inventory;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Demonstrates two complementary Resilience4j patterns:
 *
 * @CircuitBreaker on checkAvailability:
 *   After the failure-rate threshold is exceeded, the circuit OPENS and all calls
 *   return the fallback immediately without hitting the downstream service.
 *   After wait-duration-in-open-state, one probe call is allowed (HALF_OPEN).
 *   If it succeeds the circuit CLOSES; if it fails the circuit stays OPEN.
 *
 * @Retry on reserve:
 *   On exception, the call is retried up to max-attempts times with a configurable
 *   wait between attempts (fixed or exponential backoff).
 *   If all attempts fail, the fallback is invoked.
 *
 * The two annotations target separate methods here so each can be tested in isolation.
 * In production they are often stacked on the same method: the Retry is innermost
 * (retries the HTTP call), the CircuitBreaker is outermost (counts the retry batch
 * as a single failure if all retries fail).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "inventory", fallbackMethod = "availabilityFallback")
    public boolean checkAvailability(Long productId, int quantity) {
        return inventoryClient.checkAvailability(productId, quantity);
    }

    boolean availabilityFallback(Long productId, int quantity, Exception e) {
        log.warn("Circuit breaker fallback: productId={}, reason={}", productId, e.getMessage());
        return false;
    }

    @Retry(name = "inventory", fallbackMethod = "reserveFallback")
    public boolean reserve(Long productId, int quantity) {
        return inventoryClient.reserve(productId, quantity);
    }

    boolean reserveFallback(Long productId, int quantity, Exception e) {
        log.warn("Retry exhausted: productId={}, reason={}", productId, e.getMessage());
        return false;
    }
}
