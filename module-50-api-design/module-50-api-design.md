---
title: "Module 50 — API Design"
parent: "Phase 6 — Production & Architecture"
nav_order: 50
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-50-api-design/src){: .btn .btn-outline }

# Module 50 — API Design

## What this module covers

OpenAPI 3 documentation via springdoc, URI-based REST versioning, and
consumer-driven contract testing with Pact JVM. The Pact workflow demonstrates
how a consumer defines its expectations, generates a pact file, and how the
provider verifies against it — all within a single Maven build.

---

## Project structure

```
src/main/java/com/javatraining/apidesign/
├── ApiDesignApplication.java
├── product/
│   ├── Product.java                      # internal domain record
│   └── ProductRepository.java            # in-memory store
└── api/
    ├── v1/
    │   ├── ProductSummary.java            # V1 response: id, name, price
    │   └── ProductControllerV1.java       # GET /v1/products/{id}
    └── v2/
        ├── ProductDetail.java             # V2 response: + category, inStock
        └── ProductControllerV2.java       # GET /v2/products/{id}
```

---

## REST versioning

URI versioning embeds the version in the path. Two separate controllers own
their response shape — V2 adds `category` and `inStock` without modifying V1.

```
GET /v1/products/1  →  {"id":1,"name":"Widget","price":9.99}
GET /v2/products/1  →  {"id":1,"name":"Widget","price":9.99,"category":"Tools","inStock":true}
```

Breaking changes ship in a new version; existing consumers continue hitting
the old endpoint unaffected.

---

## OpenAPI 3 / Swagger

`springdoc-openapi-starter-webmvc-ui` auto-generates `/v3/api-docs` and exposes
Swagger UI at `/swagger-ui.html`.

Annotations add human-readable metadata without changing runtime behaviour:

```java
@Tag(name = "Products V1", description = "Basic product catalogue — id, name, price")
@RestController
@RequestMapping("/v1/products")
public class ProductControllerV1 {

    @Operation(summary = "Get product by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = ProductSummary.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductSummary> getById(@PathVariable Long id) { ... }
}
```

`@Schema` on the response record documents individual fields:

```java
@Schema(description = "Basic product information")
public record ProductSummary(
    @Schema(description = "Unique product identifier") Long id,
    @Schema(description = "Product name")              String name,
    @Schema(description = "Unit price")                BigDecimal price
) {}
```

---

## Consumer-driven contract testing with Pact

### What problem Pact solves

Integration tests verify that a provider works, but they do not verify that
the provider satisfies what each consumer actually needs. Pact makes the
consumer's expectations explicit and verifiable on the provider side.

### Workflow

1. **Consumer test** — defines a contract (interactions), validates consumer
   code against a mock server, and writes `target/pacts/<consumer>-<provider>.json`.
2. **Provider test** — starts the real Spring Boot application, reads the pact
   file, and replays each interaction against the running server.

Maven Surefire is configured with `runOrder=alphabetical`, which ensures
`ProductApiConsumerPactTest` (C < P) runs before `ProductApiProviderPactTest`.

### Consumer

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "ProductProvider", pactVersion = PactSpecVersion.V3)
class ProductApiConsumerPactTest {

    @Pact(consumer = "ProductConsumer")
    RequestResponsePact productSummaryShape(PactDslWithProvider builder) {
        return builder
                .given("product with id 1 exists")
                .uponReceiving("a GET request for product 1 via v1 API")
                .path("/v1/products/1")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .numberType("id", 1L)
                        .stringType("name", "Widget")
                        .decimalType("price", 9.99))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "productSummaryShape")
    void consumer_expects_product_summary_with_id_name_price(MockServer mockServer) {
        Map<String, Object> body = new RestTemplate()
                .getForObject(mockServer.getUrl() + "/v1/products/1", Map.class);

        assertThat(body).containsKeys("id", "name", "price");
    }
}
```

`PactDslJsonBody` type matchers (`.numberType`, `.stringType`, `.decimalType`)
record the expected types, not exact values — the contract survives changes to
the actual data as long as the field names and types stay the same.

`pactVersion = PactSpecVersion.V3` keeps the `RequestResponsePact + PactDslWithProvider`
API. Pact JVM 4.6.x defaults to V4 which requires a different builder signature
(`V4Pact + PactBuilder`).

### Provider

```java
@Provider("ProductProvider")
@PactFolder("target/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiProviderPactTest {

    @LocalServerPort int port;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("product with id 1 exists")
    void productWithId1Exists() {
        // ProductRepository pre-loads product 1 — no setup needed
    }
}
```

`@TestTemplate` + `PactVerificationInvocationContextProvider` generates one JUnit
test invocation per interaction in the pact file. `@State` methods set up the
server-side precondition described by the consumer's `given(...)` clause.

---

## Controller unit tests

`@WebMvcTest` slices only load the specified controller, keeping tests fast.

```java
@WebMvcTest(ProductControllerV1.class)
class ProductControllerV1Test {

    @Autowired MockMvc mockMvc;
    @MockBean  ProductRepository productRepository;

    @Test
    void getById_returns_summary_with_id_name_price_only() throws Exception {
        when(productRepository.findById(1L))
            .thenReturn(Optional.of(new Product(1L, "Widget", new BigDecimal("9.99"), "Tools", true)));

        mockMvc.perform(get("/v1/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.category").doesNotExist())   // V1 hides category
            .andExpect(jsonPath("$.inStock").doesNotExist());   // V1 hides inStock
    }
}
```

---

## Tests

| Class                        | Type              | Count |
|------------------------------|-------------------|-------|
| `ProductControllerV1Test`    | `@WebMvcTest`     | 2     |
| `ProductControllerV2Test`    | `@WebMvcTest`     | 2     |
| `ProductApiConsumerPactTest` | Pact consumer     | 1     |
| `ProductApiProviderPactTest` | Pact provider     | 1     |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **6/6 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| URI versioning (`/v1/`, `/v2/`) over header versioning | Explicit in URLs, easy to bookmark, visible in logs and proxies |
| Separate controller classes per version | Each version owns its mapping logic; no shared code to accidentally break |
| `pactVersion = PactSpecVersion.V3` | Pact 4.6.x defaults to V4; V3 keeps the familiar `RequestResponsePact + PactDslWithProvider` API |
| `runOrder=alphabetical` in Surefire | Guarantees consumer test (file generation) runs before provider test (file reading) in the same Maven build |
| Type matchers in pact DSL (`numberType`, `stringType`) | Contract survives value changes — only field existence and type are enforced |
| In-memory `ProductRepository` | Module focuses on API concerns; no JPA/H2 overhead |
{% endraw %}
