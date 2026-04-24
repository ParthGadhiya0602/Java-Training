package com.javatraining.springcloud.circuitbreaker;

import com.javatraining.springcloud.inventory.InventoryClient;
import com.javatraining.springcloud.inventory.InventoryService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests use the tight thresholds from src/test/resources/application.properties:
 *   sliding-window-size=2, minimum-number-of-calls=2, failure-rate-threshold=100%
 *   wait-duration-in-open-state=100ms
 *
 * This means two consecutive failures open the circuit immediately.
 */
@SpringBootTest
class CircuitBreakerTest {

    @Autowired InventoryService inventoryService;
    @Autowired CircuitBreakerRegistry circuitBreakerRegistry;
    @MockBean InventoryClient inventoryClient;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("inventory").reset();
    }

    @Test
    void circuit_opens_after_failure_rate_threshold_is_reached() {
        when(inventoryClient.checkAvailability(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        inventoryService.checkAvailability(1L, 1);
        inventoryService.checkAvailability(1L, 1);

        assertThat(circuitBreakerRegistry.circuitBreaker("inventory").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void open_circuit_returns_fallback_without_reaching_client() {
        when(inventoryClient.checkAvailability(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"));

        inventoryService.checkAvailability(1L, 1);
        inventoryService.checkAvailability(1L, 1); // circuit opens here

        boolean result = inventoryService.checkAvailability(1L, 1); // short-circuited

        assertThat(result).isFalse();
        verify(inventoryClient, times(2)).checkAvailability(anyLong(), anyInt());
    }

    @Test
    void circuit_closes_after_successful_probe_in_half_open() throws InterruptedException {
        when(inventoryClient.checkAvailability(anyLong(), anyInt()))
                .thenThrow(new RuntimeException("Service unavailable"))
                .thenThrow(new RuntimeException("Service unavailable"))
                .thenReturn(true); // probe call succeeds

        inventoryService.checkAvailability(1L, 1);
        inventoryService.checkAvailability(1L, 1); // circuit opens

        Thread.sleep(150); // wait > wait-duration-in-open-state (100ms)

        inventoryService.checkAvailability(1L, 1); // probe — transitions OPEN → HALF_OPEN → CLOSED

        assertThat(circuitBreakerRegistry.circuitBreaker("inventory").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
