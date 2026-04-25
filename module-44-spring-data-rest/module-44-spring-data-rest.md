---
title: "Module 44 - Spring Data REST"
parent: "Phase 5 - Spring Ecosystem"
nav_order: 44
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-44-spring-data-rest/src){: .btn .btn-outline }

# Module 44 - Spring Data REST

## Overview

Spring Data REST auto-generates a hypermedia-driven REST API directly from JPA repositories.
No controller code is required. The framework follows the HAL (Hypertext Application Language)
convention: every response includes `_links` that tell clients how to navigate the API.

---

## 1. How it works

```
JpaRepository<Product, Long>
        │
        ▼
Spring Data REST
        │
        ├── GET    /api/products         - paginated collection
        ├── POST   /api/products         - create
        ├── GET    /api/products/{id}    - single item
        ├── PUT    /api/products/{id}    - full replace
        ├── PATCH  /api/products/{id}    - partial update
        ├── DELETE /api/products/{id}    - delete
        ├── GET    /api/products/search  - list of exported search methods
        └── GET    /api/profile/products - ALPS schema (HAL-FORMS)
```

Add one dependency; zero controller code:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```

---

## 2. `@RepositoryRestResource`

```java
@RepositoryRestResource(
    collectionResourceRel = "products",   // key in _embedded: { "products": [...] }
    path = "products"                     // URL segment: /api/products
)
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Exposed as: GET /api/products/search/findByCategory?category=Electronics
    List<Product> findByCategory(@Param("category") String category);

    // @RestResource(exported = false) - hides from HTTP but still callable from Java
    @RestResource(exported = false)
    List<Product> findByActiveTrue();
}
```

**Without `@RepositoryRestResource`** the endpoint is still generated - the annotation only
adds control over the path and `_embedded` key.

**Derived query methods** are auto-exported as search endpoints. The `@Param` annotation
gives each parameter its URL name. The search discovery endpoint lists all exported methods:

```
GET /api/products/search
→ { "_links": { "findByCategory": { "href": ".../search/findByCategory{?category}" } } }
```

---

## 3. HAL response format

```
GET /api/products
```

```json
{
  "_embedded": {
    "products": [
      {
        "id": 1,
        "name": "Laptop",
        "category": "ELECTRONICS",
        "price": 999.0,
        "active": true,
        "_links": {
          "self": { "href": "http://localhost/api/products/1" },
          "product": {
            "href": "http://localhost/api/products/1{?projection}",
            "templated": true
          }
        }
      }
    ]
  },
  "_links": {
    "self": {
      "href": "http://localhost/api/products{?page,size,sort}",
      "templated": true
    },
    "profile": { "href": "http://localhost/api/profile/products" },
    "search": { "href": "http://localhost/api/products/search" }
  },
  "page": {
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "number": 0
  }
}
```

**Key HAL concepts:**

- `_embedded` - the collection of resources, keyed by `collectionResourceRel`
- `_links` - hypermedia controls: self, next/prev pages, profile, search
- `page` - pagination metadata (only present for paged repositories)
- `{?page,size,sort}` - URI templates; the `templated: true` flag signals this

**PUT vs PATCH:**

- `PUT` replaces the entire resource - omit a field and it becomes `null`
- `PATCH` merges - only supplied fields are changed, others keep their current values

---

## 4. Configuration

### application.properties

```properties
# Base path for all auto-generated endpoints
spring.data.rest.base-path=/api

# Return saved entity in the response body (default: false - returns only Location header)
spring.data.rest.return-body-on-create=true
spring.data.rest.return-body-on-update=true

spring.data.rest.default-page-size=20
```

### `RepositoryRestConfigurer`

For settings not available as properties:

```java
@Configuration
public class DataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config,
                                                      CorsRegistry cors) {
        // Include the numeric id field in the response body.
        // Without this, id is only accessible via _links.self URL.
        config.exposeIdsFor(Product.class);

        // Register projections that live outside the entity package
        config.getProjectionConfiguration().addProjection(ProductSummary.class);
    }
}
```

---

## 5. Projections

A projection is a read-only interface that limits which fields are returned - useful for
list views or public APIs that should not expose internal fields.

```java
@Projection(name = "summary", types = Product.class)
public interface ProductSummary {
    Long getId();
    String getName();
    BigDecimal getPrice();
    // category and active are omitted intentionally
}
```

```
GET /api/products/1?projection=summary
→ { "id": 1, "name": "Laptop", "price": 999.00, "_links": { ... } }
  (no category, no active)

GET /api/products?projection=summary
→ all items in the collection are projected
```

**Auto-discovery rule:** Spring Data REST automatically discovers `@Projection` interfaces
in the same package as the entity class (or sub-packages). For projections in other packages
(e.g., a dedicated `projection` package), register them explicitly:

```java
config.getProjectionConfiguration().addProjection(ProductSummary.class);
```

**Excerpt projections** apply the projection automatically to collection resources:

```java
@RepositoryRestResource(excerptProjection = ProductSummary.class)
public interface ProductRepository extends JpaRepository<Product, Long> { ... }
```

With this, `GET /api/products` returns summaries by default; full detail is still
available at `GET /api/products/{id}`.

---

## 6. Event handlers

Repository event handlers let you intercept the lifecycle of an entity before/after
it is persisted, updated, or deleted - without touching the auto-generated endpoints.

```java
@Component
@RepositoryEventHandler          // receives events for all entity types (scoped by param type)
public class ProductEventHandler {

    @HandleBeforeCreate           // fires on POST - before INSERT
    public void handleBeforeCreate(Product product) {
        product.setActive(true);
        product.setCategory(product.getCategory().toUpperCase());  // normalise on create
    }

    @HandleBeforeSave             // fires on PUT/PATCH - before UPDATE
    public void handleBeforeSave(Product product) {
        product.setCategory(product.getCategory().toUpperCase());  // normalise on update too
    }

    @HandleAfterCreate
    public void handleAfterCreate(Product product) {
        log.info("Product created: id={}", product.getId());
    }

    @HandleAfterDelete
    public void handleAfterDelete(Product product) {
        log.info("Product deleted: id={}", product.getId());
    }
}
```

**Event types:**

| Annotation              | Fires when                   |
| ----------------------- | ---------------------------- |
| `@HandleBeforeCreate`   | POST, before INSERT          |
| `@HandleAfterCreate`    | POST, after INSERT           |
| `@HandleBeforeSave`     | PUT / PATCH, before UPDATE   |
| `@HandleAfterSave`      | PUT / PATCH, after UPDATE    |
| `@HandleBeforeDelete`   | DELETE, before DELETE        |
| `@HandleAfterDelete`    | DELETE, after DELETE         |
| `@HandleBeforeLinkSave` | Association PUT, before save |

**@HandleBeforeCreate vs @HandleBeforeSave:**

- Create events fire only on POST (new entity)
- Save events fire on PUT and PATCH (update existing entity)
- Setting defaults in `@HandleBeforeCreate` ensures they only apply to new records

---

## 7. ALPS and HAL-FORMS

Spring Data REST auto-generates a profile endpoint that describes the API schema:

```
GET /api/profile/products            → application/alps+json  (ALPS metadata)
GET /api/profile/products            → application/schema+json (JSON Schema)
```

ALPS (Application-Level Profile Semantics) describes available actions, their input types,
and allowed values - machine-readable API documentation.

HAL-FORMS (`application/prs.hal-forms+json`) extends HAL with `_templates` that describe
how to build requests (like HTML `<form>` elements, but in JSON). Access it via:

```
GET /api/products/1     Accept: application/prs.hal-forms+json
→ { "_links": { ... }, "_templates": { "default": { "method": "put", "properties": [...] } } }
```

---

## 8. Testing Spring Data REST

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ProductRestTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository repository;

    @BeforeEach
    void cleanup() {
        repository.deleteAll();   // no auto-rollback with @SpringBootTest
    }

    @Test
    void get_all_returns_hal_embedded_collection() throws Exception {
        repository.save(Product.builder().name("Laptop").category("ELECTRONICS")
                .price(new BigDecimal("999.00")).active(true).build());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.products", hasSize(1)))
                .andExpect(jsonPath("$._links.self.href", notNullValue()))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    void post_event_handler_normalises_category() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"name":"Keyboard","category":"accessories","price":89.00}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category", is("ACCESSORIES")))  // event handler uppercased
                .andExpect(jsonPath("$.active", is(true)));             // event handler set default
    }

    @Test
    void patch_updates_only_supplied_fields() throws Exception {
        Product saved = repository.save(...);

        mockMvc.perform(patch("/api/products/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"price":849.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price", is(849.00)))
                .andExpect(jsonPath("$.name", is("Laptop")));  // unchanged
    }

    @Test
    void projection_returns_subset_of_fields() throws Exception {
        Product saved = repository.save(...);

        mockMvc.perform(get("/api/products/" + saved.getId() + "?projection=summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", notNullValue()))
                .andExpect(jsonPath("$.category").doesNotExist())  // excluded by projection
                .andExpect(jsonPath("$.active").doesNotExist());
    }
}
```

**Why `@SpringBootTest` and not `@WebMvcTest`?**
Spring Data REST is not a `@Controller` class - it registers endpoints via its own
auto-configuration at startup. `@WebMvcTest` only loads annotated controllers and skips
the SDR infrastructure entirely, so the endpoints would return 404. Use `@SpringBootTest(MOCK)`
with `@AutoConfigureMockMvc` to get the full context including SDR, without starting a real port.

---

## Key takeaways

- Spring Data REST generates full CRUD + search endpoints from a `JpaRepository` with zero
  controller code; endpoints follow HAL conventions with `_embedded` and `_links`
- `@RepositoryRestResource` controls the URL path and `_embedded` key;
  `@RestResource(exported = false)` hides individual methods from the HTTP API
- `PUT` replaces the entire resource; `PATCH` merges - only supplied fields change
- Projections limit which fields are returned (`?projection=name`); register them via
  `config.getProjectionConfiguration().addProjection(...)` if they are outside the entity package
- `@RepositoryEventHandler` methods intercept Create/Save/Delete lifecycle events - the
  correct place to set defaults, normalise data, or enforce invariants before persistence
- Use `config.exposeIdsFor(Entity.class)` to include the numeric id in the response body;
  by default it is only reachable via `_links.self`
- Test with `@SpringBootTest(MOCK) + @AutoConfigureMockMvc` - `@WebMvcTest` misses the SDR
  auto-configuration and all endpoints return 404
  {% endraw %}
