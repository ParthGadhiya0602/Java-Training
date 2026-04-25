---
title: "Module 27 - Integration Testing"
parent: "Phase 3 - Intermediate Engineering"
nav_order: 27
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-27-integration-testing/src){: .btn .btn-outline }

# Module 27 - Integration Testing

Integration tests verify that components work correctly together with real
infrastructure - a real database, a real HTTP stack, a real message broker.
This module covers two tools that make this practical in Java:

| Tool               | Role                                                    |
| ------------------ | ------------------------------------------------------- |
| **REST-assured**   | Fluent HTTP client DSL for asserting REST API responses |
| **Testcontainers** | Manages Docker containers from JUnit test code          |

---

## Why Integration Tests?

Unit tests with mocks verify that code compiles and individual methods behave
correctly in isolation. Integration tests catch a different class of bug:

- SQL that works in H2 fails on PostgreSQL (different type system, RETURNING clause, BIGSERIAL)
- HTTP serialisation / deserialisation edge cases (number types, null fields)
- Repository code that queries correctly but maps columns wrong
- Transactional behaviour under concurrent requests

The rule of thumb: mock at the boundary of your own code; use real infrastructure
for anything that involves a third-party system.

---

## Embedded HTTP Server for REST-assured Tests

The `ProductApiServer` uses the JDK's built-in `com.sun.net.httpserver.HttpServer`
(stable since Java 6, no external dependency):

```java
HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
server.createContext("/api/products", this::dispatch);
server.start();
int port = server.getAddress().getPort();  // OS-assigned free port
```

Binding to port `0` lets the OS pick a free port, eliminating port-conflict
flakiness between test runs.

---

## REST-assured Basics

REST-assured uses a given / when / then BDD style that reads like a specification:

```java
given()
    .contentType(ContentType.JSON)
    .body(Map.of("name", "Widget", "price", 9.99, "category", "gadgets"))
.when()
    .post("/api/products")
.then()
    .statusCode(201)
    .body("id",       greaterThan(0))
    .body("name",     equalTo("Widget"))
    .body("category", equalTo("gadgets"));
```

---

## Test Lifecycle for REST-assured

```java
static ProductApiServer server;
static InMemoryProductRepository repository;

@BeforeAll
static void startServer() {
    repository = new InMemoryProductRepository();
    server     = new ProductApiServer(new ProductService(repository));
    server.start(0);

    RestAssured.port    = server.port();  // configure base URL once
    RestAssured.baseURI = "http://localhost";
}

@AfterAll static void stopServer() { server.stop(); }

@BeforeEach void clearData()       { repository.clear(); }   // test isolation
```

Starting the server once in `@BeforeAll` avoids the overhead of restarting it for
every test. `@BeforeEach` clears the in-memory repository so each test has a clean
slate.

---

## REST-assured - Key Patterns

### Path parameters

```java
when().get("/api/products/{id}", 42).then().statusCode(200);
```

### Query parameters

```java
when().get("/api/products?category=gadgets")
    .then().body("$", hasSize(2));
```

### Status codes

```java
.then().statusCode(201)   // Created
.then().statusCode(204)   // No Content
.then().statusCode(404)   // Not Found
.then().statusCode(400)   // Bad Request
```

### Body assertions with GPath + Hamcrest

```java
.body("$",    hasSize(3))              // array size
.body("$",    empty())                 // empty array
.body("name", equalTo("Widget"))       // top-level field
.body("name", hasItems("A", "B"))      // list contains values
.body("name", not(hasItem("X")))       // list does not contain
.body("id",   greaterThan(0))          // numeric comparison
```

### Extracting values for chained requests

```java
int id =
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "Gadget", "price", 24.99, "category", "tech"))
    .when()
        .post("/api/products")
    .then()
        .statusCode(201)
        .extract().path("id");   // pull typed value from response JSON

when().get("/api/products/{id}", id).then().statusCode(200);
```

---

## Testcontainers - Core Concepts

Testcontainers starts a real Docker container from your JUnit test code and tears
it down automatically when the test suite finishes.

### Annotations

```java
@Testcontainers(disabledWithoutDocker = true)  // skip gracefully if Docker absent
class PostgresProductRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("tester")
            .withPassword("secret");
}
```

`@Container` on a **static** field: one container shared across all tests in the
class (started once).

`@Container` on an **instance** field: a fresh container for every test method
(more isolation, much slower).

### Lifecycle

```
@BeforeAll (static @Container)
  ↓ Docker pull + container start
  ↓ All test methods (share one container)
@AfterAll
  ↓ Container stop + remove
```

### Connecting to the container

```java
conn = DriverManager.getConnection(
    postgres.getJdbcUrl(),     // e.g. jdbc:postgresql://localhost:54321/testdb
    postgres.getUsername(),    // "tester"
    postgres.getPassword()     // "secret"
);
```

The `getJdbcUrl()` includes the mapped port (random each run).

---

## Integration Test Lifecycle for a Database

```java
@BeforeEach
void setUpSchemaAndConnection() throws SQLException {
    conn = DriverManager.getConnection(...);
    try (Statement st = conn.createStatement()) {
        st.execute("DROP TABLE IF EXISTS products");   // clean slate
        st.execute(JdbcProductRepository.CREATE_TABLE);
    }
    repository = new JdbcProductRepository(conn);
}

@AfterEach
void closeConnection() throws SQLException { conn.close(); }
```

Recreating the schema in `@BeforeEach` guarantees every test works with an
empty table, regardless of what other tests did.

---

## JDBC Patterns in Tests

### RETURNING clause (PostgreSQL-specific)

```java
String sql = "INSERT INTO products (name, price, category) VALUES (?, ?, ?) RETURNING id";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, name);
    ps.setBigDecimal(2, BigDecimal.valueOf(price));
    ps.setString(3, category);
    try (ResultSet rs = ps.executeQuery()) {   // use executeQuery with RETURNING
        rs.next();
        long generatedId = rs.getLong("id");
    }
}
```

Testing against real PostgreSQL catches:

- `BIGSERIAL` auto-increment behaviour (always positive, always unique, always increasing)
- `DECIMAL(10,2)` precision rounding
- `RETURNING` clause (not supported in H2 by default)

---

## disabledWithoutDocker

```java
@Testcontainers(disabledWithoutDocker = true)
class MyContainerTest { ... }
```

When Docker is not available (e.g. CI without Docker-in-Docker, or a developer
machine without Docker Desktop), the entire test class is **skipped** rather than
failing. The build stays green; the skip is visible in the test report.

---

## Container Image Strategy

```java
new PostgreSQLContainer<>("postgres:16-alpine")
```

Prefer:

- **Pinned versions** (`postgres:16-alpine`) over `latest` - reproducible builds
- **Alpine variants** where available - smaller image, faster pull
- **Official images** - well-tested, minimal attack surface

---

## Test Pyramid Recap

```
         /\
        /  \
       / E2E \      few, expensive, slow, realistic
      /--------\
     / Integration\  this module - real infra, focused scope
    /------------\
   /  Unit Tests  \  many, fast, isolated, mock dependencies
  /________________\
```

Integration tests sit in the middle: they use real infrastructure but test a
bounded scope (one repository, one service, one API layer). Keep them focused
and fast by using containers instead of shared test databases.

---

## JdbcProductRepository - Key Design Decisions

| Decision                                            | Rationale                                         |
| --------------------------------------------------- | ------------------------------------------------- |
| Accepts `Connection` (not `DataSource`)             | Tests can control transactions; simpler for demos |
| Wraps `SQLException` in `RuntimeException`          | Keeps the `ProductRepository` interface clean     |
| Uses `RETURNING id` instead of `getGeneratedKeys()` | More explicit; shows PostgreSQL-specific SQL      |
| Maps rows with a private `mapRow()` helper          | Single point of change if column names change     |

{% endraw %}
