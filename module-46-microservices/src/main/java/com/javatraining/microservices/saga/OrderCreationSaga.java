package com.javatraining.microservices.saga;

import com.javatraining.microservices.inventory.InventoryClient;
import com.javatraining.microservices.order.Order;
import com.javatraining.microservices.order.OrderRequest;
import com.javatraining.microservices.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orchestration-based saga for order creation.
 *
 * The orchestrator owns the control flow. It calls each participant (OrderService,
 * InventoryClient) in sequence and decides what to do on success or failure.
 * Participants have no knowledge of each other.
 *
 * Steps:
 *   1. Persist order as PENDING + outbox event  (local transaction)
 *   2. Reserve inventory via HTTP               (remote call — outside any transaction)
 *      ✓ success → confirm order + outbox event (local transaction)
 *      ✗ failure → cancel order + outbox event  (compensating transaction)
 *
 * The HTTP call happens between two local transactions deliberately. Holding a
 * database transaction open across a network call would block connection pool
 * threads and is an anti-pattern in distributed systems.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreationSaga {

    private final OrderService orderService;
    private final InventoryClient inventoryClient;

    public Order execute(OrderRequest request) {
        Order pending = orderService.createPendingOrder(request);
        log.info("Saga step 1: order {} created as PENDING", pending.getId());

        boolean reserved = inventoryClient.reserve(pending.getProductId(), pending.getQuantity());

        if (reserved) {
            log.info("Saga step 2: inventory reserved — confirming order {}", pending.getId());
            return orderService.confirmOrder(pending.getId());
        } else {
            log.warn("Saga step 2: inventory unavailable — cancelling order {} (compensating tx)",
                    pending.getId());
            return orderService.cancelOrder(pending.getId());
        }
    }
}
