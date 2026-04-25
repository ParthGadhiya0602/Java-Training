---
title: "Module 42 — Spring WebFlux & Reactive"
nav_order: 42
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-42-spring-webflux/src){: .btn .btn-outline }

# Module 42 — Spring WebFlux & Reactive

## Overview

Spring WebFlux is Spring's reactive web framework. It runs on Netty (non-blocking I/O) instead
of Tomcat (thread-per-request I/O). The programming model is built on **Project Reactor** —
`Mono<T>` (0 or 1 element) and `Flux<T>` (0 to N elements).

---

## Reactive vs Servlet — the core difference

```
Servlet (Spring MVC)                    Reactive (Spring WebFlux)
─────────────────────────────────────   ────────────────────────────────────────
1 thread per request (thread pool)      Few threads (Netty event loop, ~cpu cores)
Thread blocks on DB / HTTP calls        Thread never blocks — it registers a callback
Throughput limited by thread count      Throughput scales with I/O concurrency
Simpler mental model                    Requires reactive mindset (no blocking)
```

**When to choose WebFlux:**
- High-concurrency I/O: many simultaneous external calls, streaming, SSE
- Microservice fan-out: a request triggers N parallel downstream calls
- Server-Sent Events or WebSocket streaming

**When to stay with Spring MVC:**
- CRUD apps with synchronous DB access — WebFlux adds complexity without benefit
- Team unfamiliar with reactive programming — debugging reactive stacks is harder

---

## 1. Project Reactor fundamentals

```
Publisher ──onSubscribe()──► Subscriber
         ◄──request(n)──────
         ──onNext(item)──►  (up to n times)
         ──onComplete()──►  (or onError)
```

```java
// Mono — 0 or 1 element
Mono<Product> one = Mono.just(product);
Mono<Product> empty = Mono.empty();
Mono<Product> error = Mono.error(new ProductNotFoundException(99L));

// Flux — 0 to N elements
Flux<Product> many   = Flux.just(p1, p2, p3);
Flux<Product> fromDB = repository.findAll();  // lazy — no SQL until subscribed

// Operators
Flux<String> names = many.map(Product::getName);         // sync transform
Flux<String> enriched = many.flatMap(p ->
    externalService.enrich(p).map(e -> e.name()));        // async transform (inner Mono/Flux)
Mono<Long> count = many.count();                          // aggregation
```

**`map` vs `flatMap`:**
- `map(T → R)` — synchronous transform, returns R directly
- `flatMap(T → Mono<R>)` — async transform, inner function returns a Publisher; WebFlux
  subscribes to each inner publisher and merges the results

---

## 2. Spring Data R2DBC

```java
// Entity — Spring Data annotations, NOT JPA/Hibernate
@Table("products")
@Data @Builder
public class Product {
    @Id                      // org.springframework.data.annotation.Id
    private Long id;         // null before save, populated after
    private String name;
    private BigDecimal price;
    @Builder.Default
    private boolean active = true;
}

// Repository — reactive, same naming conventions as JPA derived queries
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {
    Flux<Product> findByCategory(String category);
    Flux<Product> findByActiveTrue();
}
```

**Key R2DBC differences from JPA:**
- No `@GeneratedValue` — R2DBC reads the DB-generated ID back after INSERT
- No lazy loading — no proxies, no `LazyInitializationException`
- No ORM-level joins (`@OneToMany`) — fetch related entities with separate queries
- Schema must be created externally (DDL in `schema.sql`, Flyway, or Liquibase)

**Schema initialization (`schema.sql`):**
```sql
CREATE TABLE IF NOT EXISTS products (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(255)  NOT NULL,
    price    DECIMAL(38,2) NOT NULL,
    category VARCHAR(255),
    active   BOOLEAN       NOT NULL DEFAULT TRUE
);
```

```properties
spring.r2dbc.url=r2dbc:h2:mem:///reactivedb
spring.sql.init.mode=always    # applies schema.sql on startup
```

---

## 3. Reactive service layer

```java
@Service
public class ProductService {

    public Mono<ProductResponse> findById(Long id) {
        return repository.findById(id)
                // switchIfEmpty: if upstream completes empty → subscribe to fallback
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .map(this::toResponse);      // sync transform — use map (not flatMap)
    }

    public Mono<ProductResponse> create(ProductRequest req) {
        Product entity = Product.builder()
                .name(req.name()).category(req.category()).price(req.price()).build();
        return repository.save(entity).map(this::toResponse);
        // save() returns Mono<Product> — map transforms the element, flatMap not needed here
    }
}
```

**Rules:**
- Never call `.block()` in production code — it pins a Netty I/O thread and kills throughput
- Compose operators (`map`, `flatMap`, `switchIfEmpty`) instead of imperative if/else
- `flatMap` when the inner function returns `Mono`/`Flux`; `map` for plain values

---

## 4. WebFlux annotated controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    // Regular JSON — WebFlux buffers Flux into a JSON array for application/json
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ProductResponse> getAll() {
        return productService.findAll();
    }

    // Server-Sent Events — each Flux element emitted as "data: <json>\n\n"
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ProductResponse> streamAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Mono<ProductResponse> getById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponse> create(@RequestBody @Valid ProductRequest request) {
        return productService.create(request);
    }
}
```

---

## 5. Server-Sent Events (SSE)

SSE is a unidirectional push protocol over HTTP. The server sends a stream of `data:` events;
the client reads them in order. The connection stays open until the `Flux` completes or the
client disconnects.

```
GET /api/products/stream  Accept: text/event-stream
HTTP/1.1 200 OK  Content-Type: text/event-stream

data: {"id":1,"name":"Laptop","price":999.00,...}

data: {"id":2,"name":"Mouse","price":29.00,...}

```

**Infinite streams** (e.g., live sensor data): return a `Flux` that never completes. The
connection stays alive indefinitely — backpressure signals from the client control the rate.

---

## 6. Exception handling in WebFlux

`@RestControllerAdvice` works the same as in Spring MVC for annotated controllers.
The validation exception type changes:

| Framework | Validation exception |
|---|---|
| Spring MVC | `MethodArgumentNotValidException` |
| Spring WebFlux | `WebExchangeBindException` |

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(ProductNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getMessage());
        problem.setTitle("Product Not Found");
        return problem;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(WebExchangeBindException ex) {
        // WebExchangeBindException extends BindException — getBindingResult() is available
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

---

## 7. Testing reactive code

### StepVerifier (reactor-test)

The reactive equivalent of `assertThat(...)`. Subscribes to a publisher and asserts each signal.

```java
// Basic sequence assertion
StepVerifier.create(service.findAll())
        .assertNext(r -> assertThat(r.name()).isEqualTo("Laptop"))
        .assertNext(r -> assertThat(r.name()).isEqualTo("Mouse"))
        .verifyComplete();          // asserts onComplete after the 2 elements

// Empty stream
StepVerifier.create(service.findAll())
        .verifyComplete();          // no assertNext — expects 0 elements then complete

// Error signal
StepVerifier.create(service.findById(99L))
        .expectErrorMatches(ex ->
                ex instanceof ProductNotFoundException &&
                ex.getMessage().contains("99"))
        .verify();                  // verify() instead of verifyComplete() after expectError
```

**Why not `.block()`?**
- `.block()` discards `onError` signals as unchecked exceptions
- Cannot assert intermediate elements or the order of emissions
- Hides backpressure behaviour

### `@WebFluxTest` + `WebTestClient`

```java
@WebFluxTest({ProductController.class, GlobalExceptionHandler.class})
class ProductControllerWebFluxTest {

    @Autowired WebTestClient webTestClient;
    @MockBean ProductService productService;

    @Test
    void getById_not_found_returns_404() {
        given(productService.findById(99L))
                .willReturn(Mono.error(new ProductNotFoundException(99L)));

        webTestClient.get().uri("/api/products/99")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Product Not Found");
    }

    @Test
    void stream_endpoint_returns_text_event_stream() {
        given(productService.findAll())
                .willReturn(Flux.just(new ProductResponse(1L, "Laptop", ...)));

        webTestClient.get().uri("/api/products/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBodyList(ProductResponse.class).hasSize(1);
    }
}
```

**`WebTestClient` vs `MockMvc`:**

| | `WebTestClient` (@WebFluxTest) | `MockMvc` (@WebMvcTest) |
|---|---|---|
| Framework | WebFlux | Spring MVC |
| Transport | In-process (no TCP) | In-process (no TCP) |
| DSL | Fluent, reactive-aware | Fluent, MvcResult-based |
| SSE testing | Native support | Not supported |
| Binding to real port | `WebTestClient.bindToServer(url)` | N/A |

### `@DataR2dbcTest` — reactive repository slice

```java
@DataR2dbcTest
class ProductRepositoryTest {

    @Autowired ProductRepository repository;

    @BeforeEach
    void cleanup() {
        repository.deleteAll().block();   // block() is acceptable in test setup only
    }

    @Test
    void findByCategory_returns_only_matching() {
        repository.save(Product.builder().name("Laptop").category("Electronics")...build()).block();
        repository.save(Product.builder().name("Desk").category("Furniture")...build()).block();

        StepVerifier.create(repository.findByCategory("Electronics"))
                .assertNext(p -> assertThat(p.getCategory()).isEqualTo("Electronics"))
                .verifyComplete();
    }
}
```

No automatic transaction rollback in `@DataR2dbcTest` (unlike `@DataJpaTest`) — use
`@BeforeEach deleteAll()` for clean state between tests.

---

## Dependency setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-h2</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- reactor-test is NOT transitive via spring-boot-starter-test — add explicitly -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Key takeaways

- `Mono<T>` = 0 or 1 element; `Flux<T>` = 0 to N elements — both are lazy (nothing happens
  until subscribed)
- Use `map` for synchronous transforms; `flatMap` when the inner function itself returns a
  `Mono`/`Flux`
- `switchIfEmpty(Mono.error(...))` is the reactive pattern for "throw if not found"
- Never `.block()` in production WebFlux code — it pins Netty's I/O thread
- SSE: `produces = TEXT_EVENT_STREAM_VALUE` streams each `Flux` element as a `data:` event
- `@WebFluxTest` + `WebTestClient` for controller slices; `StepVerifier` for service/reactive
  chain tests; `@DataR2dbcTest` for repository slices
- `reactor-test` must be declared explicitly — it is not pulled in by `spring-boot-starter-test`
