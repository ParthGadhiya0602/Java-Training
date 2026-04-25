---
title: "Module 46 — Microservices Architecture"
nav_order: 46
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-46-microservices/src){: .btn .btn-outline }

# Module 46 — Microservices Architecture

## Overview

A microservices architecture decomposes a system into independently deployable services,
each owning its own data. The benefits — independent scaling, isolated failures, small
deployment units — come with a fundamental cost: there is no longer a single shared
database, so distributed consistency requires careful design.

This module covers the two most important patterns for handling that cost:
the **transactional outbox** (reliable event publishing) and the **saga**
(multi-step distributed operation with compensating transactions).

---

## 1. Service decomposition

The core decomposition principle is **bounded context**: each service owns a slice of
the domain and its data store. No service reads another's database directly.

Common decomposition strategies:

| Strategy | Split by | Example |
|---|---|---|
| Business capability | What the business does | Orders, Inventory, Payments |
| Subdomain (DDD) | Domain expert's mental model | Catalogue, Fulfilment, Finance |
| Strangler fig | Incrementally replace a monolith | Route /orders to new service first |

**Rules of thumb:**
- A service should be changeable and deployable without coordinating with other teams
- If two pieces of data must be updated in the same transaction, they belong in the same service
- Start with fewer, larger services; split when team or deployment friction demands it

---

## 2. Inter-service communication

### Synchronous: `RestClient` (Spring Boot 3.2+)

`RestClient` is the synchronous successor to `RestTemplate`, with a fluent API
similar to `WebClient` but blocking.

```java
@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${inventory.service.url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean reserve(Long productId, int quantity) {
        try {
            restClient.post()
                    .uri("/inventory/reserve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ReservationRequest(productId, quantity))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            return false;   // caller decides how to compensate
        }
    }
}
```

`RestClient.Builder` is auto-configured as a **prototype** bean — each client gets
its own instance with its own `baseUrl`, independent of others.

### Asynchronous: messaging

Synchronous calls couple the caller to the availability of the downstream service.
Messaging (Kafka, RabbitMQ) decouples them: the caller publishes an event and continues
without waiting. The downstream service processes the event when it is ready.

The outbox pattern (Section 4) is the safe way to publish those events.

---

## 3. Saga pattern

A **saga** is a sequence of local transactions, each within a single service's database,
that together implement a distributed operation. Because there is no two-phase commit
across services, sagas achieve eventual consistency through **compensating transactions**
that undo the effects of earlier steps on failure.

### Choreography vs orchestration

| | Choreography | Orchestration |
|---|---|---|
| Control | Each service reacts to events | A central orchestrator drives the flow |
| Coupling | Services know about events, not each other | Orchestrator knows all participants |
| Observability | Harder — flow is implicit | Easier — flow is explicit in one place |
| Best for | Simple, few steps | Complex flows, clear rollback logic |

### Orchestration-based saga

```
OrderCreationSaga.execute(request)
  │
  ├── 1. orderService.createPendingOrder(request)    ← local tx: Order(PENDING) + OutboxEvent
  │
  ├── 2. inventoryClient.reserve(productId, qty)     ← HTTP call (outside any transaction)
  │        │
  │        ├── true  → orderService.confirmOrder(id) ← local tx: Order(CONFIRMED) + OutboxEvent
  │        └── false → orderService.cancelOrder(id)  ← compensating tx: Order(CANCELLED) + OutboxEvent
```

```java
@Component
@RequiredArgsConstructor
public class OrderCreationSaga {

    private final OrderService orderService;
    private final InventoryClient inventoryClient;

    public Order execute(OrderRequest request) {
        Order pending = orderService.createPendingOrder(request);

        boolean reserved = inventoryClient.reserve(pending.getProductId(), pending.getQuantity());

        return reserved
                ? orderService.confirmOrder(pending.getId())
                : orderService.cancelOrder(pending.getId());
    }
}
```

**Why the HTTP call is between two local transactions:**
Holding a database connection open while waiting for a network call blocks the connection
pool. The PENDING state and outbox event make the intermediate state explicit and
recoverable if the process crashes between the two transactions.

---

## 4. Transactional outbox pattern

Publishing an event to a message broker and writing to the database must be atomic.
If the database write succeeds but the broker publish fails (or the process crashes),
the event is lost forever. If the broker publish happens first and the database write
fails, a spurious event is published.

The outbox pattern solves this by writing the event to an `outbox_events` table
**in the same transaction** as the domain change. A separate publisher polls for
unpublished events and forwards them to the broker.

```
Writer (saga)                         Outbox publisher (scheduled)
─────────────────────────────────     ──────────────────────────────────
BEGIN TX                              SELECT * FROM outbox_events
  INSERT INTO orders (status=PENDING)   WHERE published = false;
  INSERT INTO outbox_events (...)     
COMMIT                                for each event:
                                        publish to broker
                                        UPDATE outbox_events
                                          SET published = true
```

### Entities

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id @GeneratedValue Long id;
    String aggregateType;   // "Order"
    Long aggregateId;       // order id
    String eventType;       // "OrderCreated", "OrderConfirmed", "OrderCancelled"
    String payload;         // JSON
    boolean published;
    LocalDateTime createdAt;
}
```

### Atomic write (same transaction as the domain change)

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    @Transactional
    public Order createPendingOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCreated"));
        return order;
    }
}
```

### Publisher (polls and marks as sent)

```java
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalse();
        for (OutboxEvent event : pending) {
            // send to Kafka/RabbitMQ in production
            log.info("Publishing: type={}, aggregateId={}", event.getEventType(), event.getAggregateId());
            event.setPublished(true);
        }
    }
}
```

**Guarantees and trade-offs:**
- At-least-once delivery — if the publisher crashes after sending but before marking
  `published = true`, the event is re-sent on the next poll. Consumers must be idempotent.
- No event loss — an event only disappears after it has been successfully forwarded.
- Simple to implement with only a relational database — no distributed transaction coordinator.

---

## 5. Testing

### Saga — pure unit test

The saga's logic can be tested with Mockito alone: mock `OrderService` and `InventoryClient`,
verify the correct service methods are called in the correct order.

```java
@ExtendWith(MockitoExtension.class)
class OrderCreationSagaTest {

    @Mock OrderService orderService;
    @Mock InventoryClient inventoryClient;
    @InjectMocks OrderCreationSaga saga;

    @Test
    void confirms_order_when_inventory_is_available() {
        when(orderService.createPendingOrder(any())).thenReturn(pendingOrder);
        when(inventoryClient.reserve(1L, 2)).thenReturn(true);
        when(orderService.confirmOrder(1L)).thenReturn(confirmedOrder);

        Order result = saga.execute(new OrderRequest(1L, 2));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderService, never()).cancelOrder(any());
    }

    @Test
    void cancels_order_when_inventory_is_unavailable() {
        when(orderService.createPendingOrder(any())).thenReturn(pendingOrder);
        when(inventoryClient.reserve(1L, 2)).thenReturn(false);
        when(orderService.cancelOrder(1L)).thenReturn(cancelledOrder);

        Order result = saga.execute(new OrderRequest(1L, 2));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderService, never()).confirmOrder(any());
    }

    @Test
    void always_creates_pending_order_before_calling_inventory() {
        // ...
        var inOrder = inOrder(orderService, inventoryClient);
        inOrder.verify(orderService).createPendingOrder(any());
        inOrder.verify(inventoryClient).reserve(1L, 2);
        inOrder.verify(orderService).confirmOrder(1L);
    }
}
```

### Outbox pattern — `@DataJpaTest`

Verify the two writes happen in the same transaction and the query for unpublished
events works correctly.

```java
@DataJpaTest
class OutboxPatternTest {

    @Test
    void order_and_outbox_event_are_persisted_atomically() {
        Order order = orderRepository.save(Order.builder()...build());
        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCreated"));

        em.flush(); em.clear();

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.findByPublishedFalse()).hasSize(1);
    }
}
```

---

## Key takeaways

- Decompose by bounded context — each service owns its data; no shared databases
- Synchronous communication uses `RestClient` (Spring Boot 3.2+); asynchronous uses
  messaging — prefer async to avoid availability coupling between services
- Saga replaces cross-service distributed transactions with a sequence of local
  transactions and compensating transactions on failure
- Orchestration-based sagas keep the flow in one place (easier to reason about);
  choreography-based sagas have no central coordinator (looser coupling, harder to trace)
- The outbox pattern makes event publishing atomic with database writes — the event is
  written to an `outbox_events` table in the same transaction, then a poller forwards it
  to the broker. This gives at-least-once delivery without a distributed transaction coordinator
- Consumers of outbox-published events must be **idempotent**: the same event may arrive
  more than once if the publisher crashes between sending and marking the event as published
{% endraw %}
