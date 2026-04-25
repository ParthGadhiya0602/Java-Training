---
title: "Module 39 - Spring REST APIs"
parent: "Phase 5 - Spring Ecosystem"
nav_order: 39
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-39-spring-rest/src){: .btn .btn-outline }

# Module 39 - Spring REST APIs

Building a fully-featured REST API with Spring MVC:
**@RestController** for request handling,
**Bean Validation** for input constraints,
**@RestControllerAdvice** + **ProblemDetail** for standardized error responses (RFC 9457),
**HATEOAS** for hypermedia links, and
**content negotiation** to control acceptable response formats.
Tests use the **@WebMvcTest** slice - no real HTTP server, no database.

---

## @RestController Anatomy

```java
// @RestController = @Controller + @ResponseBody on every method
// Every return value is serialized directly to the response body (JSON by default).

@RestController
@RequestMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController {

    private final ProductService productService;

    // Constructor injection - no @Autowired needed (single constructor)
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ProductResponse>> getById(@PathVariable Long id) { ... }

    @PostMapping
    public ResponseEntity<EntityModel<ProductResponse>> create(
            @Valid @RequestBody ProductRequest request) { ... }

    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { ... }
}
```

```
  Key annotations:
  @PathVariable   - bind URI template segment {id} to a method parameter
  @RequestParam   - bind query string ?category=X (required=false → optional)
  @RequestBody    - deserialize JSON body into a Java object (Jackson)
  @Valid          - trigger Bean Validation before the method body runs
  ResponseEntity  - control status code + headers + body explicitly
```

---

## Content Negotiation

```java
// produces = APPLICATION_JSON_VALUE declares what this controller can produce.
// If the client sends Accept: text/html → 406 Not Acceptable.
// If the client sends Accept: application/json → 200 OK.

@RestController
@RequestMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductController { ... }
```

```
  Content negotiation:
    Client sends Accept header           Spring MVC checks produces list
    ─────────────────────────────────    ────────────────────────────────────────
    Accept: application/json         →   match → 200 OK, JSON body
    Accept: application/hal+json     →   match (HATEOAS alias) → 200 OK
    Accept: text/html                →   no match → 406 Not Acceptable
    (no Accept header)               →   defaults to first produces entry → 200 OK
```

---

## Bean Validation

```java
// Request DTO as a Java record with constraint annotations.
// @Valid on @RequestBody triggers validation before the method runs.
// If any constraint fails → MethodArgumentNotValidException (→ handled by @ControllerAdvice).

public record ProductRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
        BigDecimal price,

        @NotBlank(message = "Category is required")
        String category
) {}
```

```
  Common constraints:
  @NotNull         - value must not be null
  @NotBlank        - String must not be null, empty, or whitespace
  @Size(min, max)  - String/Collection size range
  @Min / @Max      - numeric lower/upper bound
  @DecimalMin      - BigDecimal lower bound (string, to avoid floating-point issues)
  @Email           - valid email format
  @Pattern(regexp) - regex match

  Validation happens BEFORE the controller method body runs.
  No need to check constraints manually inside the method.
```

---

## @RestControllerAdvice + ProblemDetail (RFC 9457)

```java
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody
// Catches exceptions thrown from any @RestController in the application.
// ProblemDetail is Spring 6's built-in RFC 9457 implementation.

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - resource not found
    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFound(ProductNotFoundException ex,
                                               HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Product Not Found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    // 400 - @Valid constraint violations
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex,
                                                HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);   // custom extension property
        return problem;
    }
}
```

```
  RFC 9457 ProblemDetail JSON response:
  {
    "type":     "about:blank",
    "title":    "Product Not Found",
    "status":   404,
    "detail":   "Product not found: 99",
    "instance": "/products/99"
  }

  Standard fields:   type, title, status, detail, instance
  Extension fields:  any via problem.setProperty("key", value)
  Status code:       returned as both HTTP response status AND "status" field in body
```

---

## HATEOAS

### Why HATEOAS?

```
  REST Level 3 (Richardson Maturity Model) - clients follow links, not hard-coded URLs.
  API can change URL structure without breaking clients that follow the links.
  Self-documenting: each response tells the client what it can do next.

  HAL (Hypertext Application Language) format:
  {
    "id": 1,
    "name": "Laptop",
    "_links": {
      "self":     { "href": "http://localhost/products/1" },
      "products": { "href": "http://localhost/products"  }
    }
  }
```

### EntityModel and CollectionModel

```java
// EntityModel<T> - wraps a single item with links
// CollectionModel<EntityModel<T>> - wraps a list with links

@GetMapping("/{id}")
public ResponseEntity<EntityModel<ProductResponse>> getById(@PathVariable Long id) {
    ProductResponse product = productService.findById(id);

    EntityModel<ProductResponse> model = EntityModel.of(product,
            linkTo(methodOn(ProductController.class).getById(id)).withSelfRel(),
            linkTo(methodOn(ProductController.class).getAll(null)).withRel("products"));

    return ResponseEntity.ok(model);
}

@GetMapping
public ResponseEntity<CollectionModel<EntityModel<ProductResponse>>> getAll(
        @RequestParam(required = false) String category) {

    List<EntityModel<ProductResponse>> items = products.stream()
            .map(p -> EntityModel.of(p,
                    linkTo(methodOn(ProductController.class).getById(p.id())).withSelfRel()))
            .toList();

    CollectionModel<EntityModel<ProductResponse>> collection = CollectionModel.of(items,
            linkTo(methodOn(ProductController.class).getAll(null)).withSelfRel());

    return ResponseEntity.ok(collection);
}
```

```
  linkTo(methodOn(C.class).method(args))  - type-safe link generation (no string URLs)
  .withSelfRel()                          - rel="self"
  .withRel("products")                   - custom rel name
  .toUri()                               - extract URI for Location header
```

### @Relation - Controlling the \_embedded Key

```java
// Without @Relation, HAL uses the class name → "productResponseList" (ugly)
// @Relation sets predictable keys for collection and single item

@Relation(collectionRelation = "products", itemRelation = "product")
public record ProductResponse(Long id, String name, BigDecimal price, String category) {}

// Collection response:
// {
//   "_embedded": {
//     "products": [{ "id": 1, "name": "Laptop", ... }, ...]
//   },
//   "_links": { "self": { "href": "http://localhost/products" } }
// }
```

### 201 Created with Location Header

```java
// POST /products - return 201 + Location pointing to the new resource
@PostMapping
public ResponseEntity<EntityModel<ProductResponse>> create(
        @Valid @RequestBody ProductRequest request) {

    ProductResponse created = productService.create(request);

    return ResponseEntity
            .created(linkTo(methodOn(ProductController.class).getById(created.id())).toUri())
            .body(EntityModel.of(created,
                    linkTo(methodOn(ProductController.class).getById(created.id())).withSelfRel()));
}

// Location: http://localhost/products/1
// Status: 201 Created
```

---

## @WebMvcTest - Slice Testing

```java
// @WebMvcTest loads only the web layer:
//   - @RestController beans
//   - @ControllerAdvice / @RestControllerAdvice
//   - Jackson, HATEOAS converters, Spring MVC config
//   - Does NOT load @Service, @Repository, @Component
//
// MockMvc performs mock HTTP requests - no real server, no real HTTP.
// @MockBean creates a Mockito mock and registers it in the Spring context.

@WebMvcTest
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;  // replaces the real service

    @Test
    void getById_found_returns_entity_with_links() throws Exception {
        given(productService.findById(1L)).willReturn(
                new ProductResponse(1L, "Laptop", new BigDecimal("999.00"), "Electronics"));

        mockMvc.perform(get("/products/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.products").exists());
    }

    @Test
    void create_blank_name_returns_400_problem_detail() throws Exception {
        ProductRequest bad = new ProductRequest("", new BigDecimal("999.00"), "Electronics");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
```

```
  BDDMockito patterns for void methods:
    given(service.findById(1L)).willReturn(response)     - stub return value
    given(service.findById(99L)).willThrow(new Ex(99L))  - stub exception
    willDoNothing().given(service).delete(1L)            - void method, no-op
    willThrow(new Ex(99L)).given(service).delete(99L)    - void method, throw
```

---

## Module 39 - What Was Built

```
  module-39-spring-rest/
  ├── pom.xml     (Spring Boot 3.3.5, spring-boot-starter-hateoas,
  │               spring-boot-starter-validation, lombok 1.18.34)
  └── src/
      ├── main/java/com/javatraining/springrest/
      │   ├── SpringRestApplication.java
      │   ├── model/
      │   │   └── Product.java            - @Data @Builder (in-memory entity)
      │   ├── dto/
      │   │   ├── ProductRequest.java     - record + Bean Validation constraints
      │   │   └── ProductResponse.java    - record + @Relation(collectionRelation="products")
      │   ├── exception/
      │   │   ├── ProductNotFoundException.java   - RuntimeException with id in message
      │   │   └── GlobalExceptionHandler.java     - @RestControllerAdvice, ProblemDetail
      │   ├── service/
      │   │   └── ProductService.java     - ConcurrentHashMap store, AtomicLong IDs
      │   └── controller/
      │       └── ProductController.java  - produces=APPLICATION_JSON_VALUE,
      │                                     EntityModel/CollectionModel, WebMvcLinkBuilder
      └── test/java/com/javatraining/springrest/
          └── ProductControllerTest.java  - 12 tests:
              GET all (HAL collection + links)
              GET by category (?category= filter)
              GET by id (entity + links)
              GET 404 (ProblemDetail)
              POST valid (201 + Location header)
              POST blank name (400 ProblemDetail + errors array)
              POST null price (400 ProblemDetail)
              PUT existing (200)
              PUT not found (404)
              DELETE existing (204)
              DELETE not found (404)
              Accept: text/html (406 content negotiation)
```

All tests: **12 passing**.

---

## Key Takeaways

```
  @RestController              @Controller + @ResponseBody - returns JSON by default
  produces = APPLICATION_JSON  declares acceptable response type; other Accept → 406
  @PathVariable                bind {id} segment to method parameter
  @RequestParam(required=false) optional query string; null when absent
  @Valid @RequestBody          trigger Bean Validation on the deserialized object
  ResponseEntity               explicit control of status code, headers, body

  ProblemDetail                RFC 9457: type/title/status/detail/instance + extensions
  @RestControllerAdvice        cross-cutting exception → ProblemDetail mapping
  setProperty("errors", list)  add custom extension fields to the problem response

  EntityModel.of(t, links)     wrap item with HATEOAS links
  CollectionModel.of(items, links) wrap list with HATEOAS links
  linkTo(methodOn(C.class).m()) type-safe link - no string URLs
  .withSelfRel()               rel="self" link
  .withRel("products")         custom named relation link
  .toUri()                     extract URI for Location header (POST 201)
  @Relation(collectionRelation) control HAL _embedded key name (avoid ugly defaults)

  @WebMvcTest                  web-layer-only slice: no @Service, no DB
  @MockBean                    Mockito mock registered in the slice context
  MockMvc                      mock HTTP requests without a real server
  BDDMockito                   given/willReturn, willThrow, willDoNothing patterns
```

{% endraw %}
