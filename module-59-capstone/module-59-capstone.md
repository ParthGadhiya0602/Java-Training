---
title: "Module 59 — Capstone"
nav_order: 59
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-59-capstone/src){: .btn .btn-outline }

# Module 59 — Capstone

## What this module covers

An end-to-end production system that integrates every technique from Phase 6:
REST (Spring MVC), gRPC (InventoryService), Kafka (order events), JPA (H2), Spring Security,
Micrometer metrics, virtual threads, Docker, and GitHub Actions CI. Tests exercise the full
order creation flow with embedded Kafka and an in-process gRPC server.

---

## System overview

```
┌────────────────── Spring Boot App ──────────────────────────────────────────┐
│                                                                              │
│   REST (port 8080)           gRPC (port 9090)       Kafka (topic: orders)  │
│   ─────────────────          ──────────────────      ──────────────────     │
│   POST /api/orders     ───►  InventoryService        OrderEventPublisher    │
│   GET  /api/orders/{id}       .checkStock()     ───► .publish(order)        │
│   GET  /api/orders (ADMIN)                                │                 │
│          │                                               ▼                  │
│          │                                    NotificationListener          │
│          │                                    @KafkaListener("orders")      │
│          ▼                                                                   │
│       OrderService ──► OrderRepository (JPA / H2)                           │
│       + Micrometer counters / timers                                         │
│       + Spring Security (USER / ADMIN roles)                                 │
│       + Virtual threads (spring.threads.virtual.enabled=true)                │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Order creation flow

```
Client
  │  POST /api/orders  {"productId":"PROD-1","quantity":2}
  ▼
OrderController  (@Valid)
  │
  ▼
OrderService.createOrder()
  ├─ InventoryClient.checkStock("PROD-1", 2)   ──► gRPC InventoryServiceImpl
  │                                              ◄── StockResponse{available:true}
  ├─ orderRepository.save(order)               → H2
  └─ orderEventPublisher.publish(order)        → Kafka topic "orders"
                                                      │
                                               NotificationListener
                                               @KafkaListener("orders")
                                               → logs notification
```

---

## Project structure

```
src/main/java/com/javatraining/capstone/
├── CapstoneApplication.java
├── config/
│   ├── SecurityConfig.java       # Spring Security: auth, roles, BCrypt, headers
│   └── GrpcClientConfig.java     # ManagedChannel bean for InventoryClient
├── order/
│   ├── Order.java                # JPA entity
│   ├── OrderStatus.java          # CONFIRMED / REJECTED
│   ├── OrderRepository.java      # JpaRepository<Order, Long>
│   ├── OrderRequest.java         # record: @NotBlank productId, @Positive quantity
│   ├── OrderService.java         # orchestration + Micrometer counters/timers
│   ├── OrderController.java      # REST endpoints
│   └── InsufficientStockException.java  # @ResponseStatus(422)
├── inventory/
│   ├── InventoryServiceImpl.java # @GrpcService — in-memory stock map
│   └── InventoryClient.java      # gRPC blocking stub wrapper
├── event/
│   └── OrderEventPublisher.java  # KafkaTemplate → "orders" topic
└── notification/
    └── NotificationListener.java # @KafkaListener("orders")

src/main/proto/
└── inventory.proto               # CheckStock RPC: StockRequest → StockResponse

src/main/resources/
└── application.properties        # H2, Kafka, gRPC, actuator, virtual threads

src/test/
├── resources/application.properties    # grpc.server.port=-1 (no port in tests)
└── java/com/javatraining/capstone/
    ├── InventoryGrpcServiceTest.java    # in-process gRPC, no Spring (2 tests)
    ├── OrderFlowIntegrationTest.java    # Spring + EmbeddedKafka + MockBean (3 tests)
    └── SecurityTest.java               # 401 / 403 access control (2 tests)

Dockerfile
docker-compose.yml                # app + Kafka + Zookeeper
.github/workflows/ci.yml
```

---

## gRPC — InventoryService

```protobuf
service InventoryService {
    rpc CheckStock (StockRequest) returns (StockResponse);
}
message StockRequest  { string product_id = 1; int32 quantity = 2; }
message StockResponse { bool available = 1; int32 current_stock = 2; }
```

`InventoryServiceImpl` holds a static in-memory stock map (`PROD-1 → 100`, `PROD-2 → 50`,
`PROD-3 → 200`). The gRPC server runs on port 9090 in production; in tests `grpc.server.port=-1`
disables it and `InventoryGrpcServiceTest` creates its own `InProcessServerBuilder` instance.

---

## Kafka — order events

```
Producer (OrderEventPublisher)
  KafkaTemplate<String, String>
  key   = orderId
  value = {"orderId":1,"productId":"PROD-1","quantity":2,"status":"CONFIRMED"}
  topic = "orders"

Consumer (NotificationListener)
  @KafkaListener(topics = "orders", groupId = "notifications")
  Logs the event; increments processedCount (observable in tests)
```

In tests, `@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")`
overrides the broker address. `spring.kafka.consumer.auto-offset-reset=earliest` ensures
messages produced before the consumer starts are still received.

---

## Security

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
    .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN")
    .anyRequest().authenticated()
)
.httpBasic(Customizer.withDefaults())
.csrf(AbstractHttpConfigurer::disable)   // stateless API
.headers(Customizer.withDefaults())       // DENY framing, nosniff, cache-control
```

| Endpoint | USER | ADMIN |
|---|---|---|
| `POST /api/orders` | ✅ | ✅ |
| `GET /api/orders/{id}` | ✅ | ✅ |
| `GET /api/orders` (list all) | ❌ 403 | ✅ |
| `GET /actuator/**` | ✅ (no auth) | ✅ |

---

## Observability

```java
Counter.builder("orders.created")
       .description("Total orders successfully created")
       .register(meterRegistry);

Timer.builder("orders.creation.duration")
     .description("Time spent creating an order")
     .register(meterRegistry);
```

Prometheus scrape endpoint: `GET /actuator/prometheus`

---

## Virtual threads

```properties
spring.threads.virtual.enabled=true
```

Spring Boot configures Tomcat's thread pool with `VirtualThreadExecutor`. Each HTTP
request runs on a virtual thread — I/O blocks (gRPC call, DB write, Kafka send) unmount
the carrier thread instead of stalling it, allowing far more concurrent requests than the
carrier thread count (≈ number of CPU cores).

---

## Testing approach

| Class | What's real | What's mocked | Tests |
|---|---|---|---|
| `InventoryGrpcServiceTest` | `InventoryServiceImpl` via in-process gRPC | nothing (no Spring) | 2 |
| `OrderFlowIntegrationTest` | REST → `OrderService` → H2 + Kafka | `InventoryClient` | 3 |
| `SecurityTest` | Full security filter chain | nothing | 2 |

**Why mock `InventoryClient` in `OrderFlowIntegrationTest`?**
The gRPC client connects to `localhost:9090`. Testing via an actual running gRPC server
in a Spring integration test would require port coordination and restart costs. Instead:
- `InventoryGrpcServiceTest` proves the server logic is correct
- `@MockBean InventoryClient` proves the orchestration (order creation flow) is correct
- The boundary between them is the Java interface `InventoryClient.checkStock()`

---

## Docker

```bash
docker compose up --build          # starts Zookeeper + Kafka + app
docker compose down                # tear down
```

`docker-compose.yml` uses health checks so `app` only starts after Kafka is ready.
The gRPC port 9090 and REST port 8080 are both exposed.

---

## CI (GitHub Actions)

`.github/workflows/ci.yml` runs on every push and PR:
1. `mvn --batch-mode verify` — compiles, runs all 7 tests
2. Uploads Surefire reports as an artifact
3. On `main` only: builds the Docker image (verifies the Dockerfile is valid)

---

## Tests

| Class | Coverage | Tests |
|---|---|---|
| `InventoryGrpcServiceTest` | gRPC service logic | 2 |
| `OrderFlowIntegrationTest` | REST + Kafka + JPA + Micrometer | 3 |
| `SecurityTest` | Access control (A01) | 2 |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **7/7 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `@MockBean InventoryClient` in flow tests | Decouples gRPC correctness test from orchestration test; avoids starting a gRPC server in the Spring context |
| `@EmbeddedKafka` on every Spring test class | `@KafkaListener` tries to connect on context startup; embedded broker satisfies that without a real Kafka |
| `grpc.server.port=-1` in `src/test/resources` | Prevents the `@GrpcService` auto-configuration from binding port 9090 during tests that don't need the server |
| `Awaitility.await()` for Kafka assertion | Kafka consumer runs asynchronously; `Thread.sleep` is fragile; Awaitility polls until the condition is met (included in `spring-boot-starter-test`) |
| `AtomicInteger processedCount` in `NotificationListener` | Observable without coupling the test to Kafka internals (no raw consumer / `KafkaTestUtils`) |
| `spring.threads.virtual.enabled=true` | Every Tomcat request thread is virtual; gRPC and Kafka I/O blocks unmount the carrier thread — higher throughput with the same resource footprint |
{% endraw %}
