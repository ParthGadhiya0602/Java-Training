---
title: "Module 41 — Spring Testing"
nav_order: 41
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-41-spring-testing/src){: .btn .btn-outline }

# Module 41 — Spring Testing

## Overview

Spring Testing is a layered discipline: every layer of a Spring Boot application has a dedicated,
focused test strategy. This module covers the full spectrum — from fast, isolated slices to
full-stack integration tests against real infrastructure.

---

## Testing pyramid in Spring Boot

```
           ┌──────────────────────────┐
           │  @SpringBootTest         │  Slow — full context, real HTTP
           │  (RANDOM_PORT / MOCK)    │
           ├──────────────────────────┤
           │  WireMock                │  External HTTP services
           ├──────────────────────────┤
           │  Testcontainers          │  Real DB / message broker
           ├──────────────────────────┤
           │  @WebMvcTest             │  Web slice — fast (no JPA, no service)
           │  @DataJpaTest            │  JPA slice — fast (no web, no service)
           │  @JsonTest               │  JSON only — fastest
           └──────────────────────────┘
```

---

## 1. `@JsonTest` — JSON slice

```java
@JsonTest
class ProductJsonTest {

    @Autowired ObjectMapper objectMapper;
    JacksonTester<ProductResponse> responseJson;
    JacksonTester<ProductRequest> requestJson;

    @BeforeEach
    void setUp() {
        // @JsonTest does NOT auto-wire JacksonTester<T> — init manually
        JacksonTester.initFields(this, objectMapper);
    }

    @Test
    void serializes_product_response_to_json() throws Exception {
        ProductResponse p = new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics", true);
        JsonContent<ProductResponse> content = responseJson.write(p);

        assertThat(content).hasJsonPathStringValue("$.name", "Laptop");
        // Number is Double at runtime — convert for BigDecimal comparison
        assertThat(content).extractingJsonPathNumberValue("$.price")
                .satisfies(n -> assertThat(new BigDecimal(n.toString())).isEqualByComparingTo("999.00"));
    }
}
```

**What `@JsonTest` loads:** Jackson `ObjectMapper` with all Spring Boot auto-configuration
(date/time modules, `@JsonInclude`, custom serializers from `@JsonComponent` beans).

**What it does NOT load:** Spring MVC, JPA, services — startup is sub-100 ms.

**`JacksonTester.initFields`:** scans the test instance for `JacksonTester<T>` fields and
binds each to the configured `ObjectMapper`. This step is required — `@JsonTest` does not
register them as Spring beans.

---

## 2. `@DataJpaTest` — JPA slice

```java
@DataJpaTest
class ProductRepositoryH2Test {

    @Autowired ProductRepository productRepository;
    @Autowired TestEntityManager entityManager;

    @Test
    void save_and_find_by_id() {
        Product product = Product.builder().name("Laptop").category("Electronics")
                .price(new BigDecimal("999.00")).build();

        // persist+flush writes the INSERT; clear evicts the L1 cache
        entityManager.persistAndFlush(product);
        entityManager.clear();

        // findById now issues a real SELECT instead of returning the cached object
        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Laptop");
    }
}
```

**What `@DataJpaTest` loads:** JPA entities, repositories, `TestEntityManager`, and an H2
in-memory database (replaces any configured datasource by default).

**Transaction rollback:** each test runs in a transaction that is rolled back after the test —
the database is always clean without explicit cleanup.

**`TestEntityManager` vs repository:**
- `persistAndFlush()` — bypasses the repository interface and writes SQL directly; useful to
  set up test data without testing the repository itself
- `clear()` — evicts the first-level (L1) cache so the next `findById` hits the database

---

## 3. `@WebMvcTest` — web layer slice

```java
@WebMvcTest(ProductController.class)
class ProductControllerSliceTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;

    @Test
    void getById_not_found_returns_404() throws Exception {
        given(productService.findById(99L)).willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"));
    }
}
```

**What `@WebMvcTest` loads:** the named controller, `@ControllerAdvice`, Jackson, Spring MVC
configuration — nothing else.

**`@MockBean`:** replaces the service bean in the application context with a Mockito mock.
`BDDMockito.given()` sets the stub behaviour.

**Benefit over `@SpringBootTest`:** startup in ~400 ms instead of 2+ s; a broken persistence
layer cannot fail controller tests; forces clear separation of responsibilities.

---

## 4. `@DataJpaTest` + Testcontainers — real PostgreSQL

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // (1)
@Testcontainers(disabledWithoutDocker = true)                                   // (2)
class ProductRepositoryTCTest {

    @Container
    @ServiceConnection                                                           // (3)
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired ProductRepository productRepository;

    @BeforeEach
    void cleanup() {
        productRepository.deleteAll();    // no automatic rollback from @DataJpaTest here
    }
}
```

**(1) `replace = NONE`:** prevents `@DataJpaTest` from substituting the datasource with H2.
The datasource now comes from Testcontainers.

**(2) `disabledWithoutDocker = true`:** the test suite skips gracefully on machines where Docker
is not running instead of throwing an exception.

**(3) `@ServiceConnection` (Spring Boot 3.1+):** reads host, port, database, username, and
password from the running container and auto-registers them as `DataSource` connection details —
no manual `@DynamicPropertySource` needed for standard containers.

**Static `@Container`:** the container starts once before the first test in the class and stops
after the last — all tests share the same PostgreSQL instance. Because `@DataJpaTest` transaction
rollback does not apply with an external container (since the commit happens before the assertion
in some scenarios), a `@BeforeEach` cleanup calls `deleteAll()` to ensure a clean slate.

**Why use Testcontainers at all?**
H2 is convenient but its SQL dialect diverges from PostgreSQL: window functions, `RETURNING`,
JSON operators, custom types, and index behaviour may all differ. Testcontainers tests run against
the exact engine used in production — catching dialect-specific bugs before they reach staging.

---

## 5. WireMock — HTTP stub server

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PricingClientWireMockTest {

    @RegisterExtension                                                           // (1)
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource                                                       // (2)
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("pricing.service.url", wireMock::baseUrl);
    }

    @Autowired PricingClient pricingClient;

    @Test
    void getPrice_service_unavailable_returns_zero_fallback() {
        wireMock.stubFor(get(urlEqualTo("/prices/2"))
                .willReturn(aResponse().withStatus(503)));

        BigDecimal price = pricingClient.getPrice(2L);

        assertThat(price).isEqualByComparingTo("0");    // fallback branch
    }

    @Test
    void getPrice_verifies_request_sent_to_correct_endpoint() {
        wireMock.stubFor(get(urlEqualTo("/prices/42"))
                .willReturn(okJson("{\"productId\":42,\"price\":149.99}")));

        pricingClient.getPrice(42L);

        wireMock.verify(1, getRequestedFor(urlEqualTo("/prices/42")));
    }
}
```

**(1) `@RegisterExtension static WireMockExtension`:** starts a Jetty server on a random port
before all tests; resets stubs between tests; stops after all tests.

**(2) `@DynamicPropertySource`:** runs *after* the WireMock extension starts (so
`wireMock.baseUrl()` is available) but *before* the Spring context is created — this is the
correct seam for injecting the random port. The method reference `wireMock::baseUrl` is a
`Supplier<String>` called when the property is resolved, not immediately.

**`webEnvironment = NONE`:** no web server is started — only the beans needed to wire
`PricingClient` (and its dependencies) are loaded, making the test fast.

**WireMock vs `@MockBean`:**

| Aspect | `@MockBean` | WireMock |
|---|---|---|
| What is mocked | Java object | Real HTTP server |
| Real HTTP happens | No | Yes |
| Tests URL construction | No | Yes |
| Tests headers/serialization | No | Yes |
| Tests timeout/retry config | No | Yes |
| Catches JSON field name bugs | No | Yes |

---

## 6. `@SpringBootTest` — full integration

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @MockBean PricingClient pricingClient;   // external service — not the subject under test

    @Test
    void create_then_retrieve_roundtrip() {
        ResponseEntity<ProductResponse> created = restTemplate.postForEntity(
                "/api/products",
                new ProductRequest("Laptop", new BigDecimal("999.00"), "Electronics"),
                ProductResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ProductResponse> retrieved = restTemplate.getForEntity(
                "/api/products/" + created.getBody().id(), ProductResponse.class);

        assertThat(retrieved.getBody().name()).isEqualTo("Laptop");
    }
}
```

**`RANDOM_PORT`:** starts the embedded Tomcat on a random free port; `TestRestTemplate` is
auto-configured to call it over TCP — the full servlet container, filter chain, and connection
handling are exercised.

**No automatic rollback:** `@SpringBootTest` does not wrap tests in rolled-back transactions.
Each test must create its own data and not depend on cross-test state.

**`TestRestTemplate` vs `MockMvc`:**
- `MockMvc` — in-process; does not start a real port; faster for slice tests
- `TestRestTemplate` — real TCP; exercises the container layer; needed for `RANDOM_PORT` tests

---

## Spring context caching

Spring reuses a loaded context for all tests that share the same configuration key. Anything that
changes the key forces a new context: `@MockBean`, `@DynamicPropertySource`, different
`@ActiveProfiles`, different properties files.

| Annotation | Context scope |
|---|---|
| `@SpringBootTest` | Full context (heaviest) |
| `@WebMvcTest` | Web slice |
| `@DataJpaTest` | JPA slice |
| `@JsonTest` | ObjectMapper only |
| `@DynamicPropertySource` | New context per unique value set |

Keep the number of distinct context configurations low — each unique combination triggers a fresh
Spring startup.

---

## Key takeaways

- Match test granularity to the layer under test: `@JsonTest` for serialization, `@DataJpaTest`
  for repositories, `@WebMvcTest` for controllers, WireMock for HTTP clients, Testcontainers for
  DB-dialect-specific behaviour, `@SpringBootTest(RANDOM_PORT)` for the full stack.
- `@DataJpaTest` rolls back automatically — no cleanup needed. `@SpringBootTest` does not.
- Use `@ServiceConnection` with Testcontainers to eliminate manual `@DynamicPropertySource`
  boilerplate for standard containers.
- WireMock tests what `@MockBean` cannot: URL construction, headers, serialization, timeouts,
  and retry logic all run as in production.
- `@DynamicPropertySource` runs after extensions but before context creation — the correct seam
  for injecting dynamic infrastructure coordinates (ports, connection strings) into Spring.
