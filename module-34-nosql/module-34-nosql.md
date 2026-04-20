---
title: "Module 34 — NoSQL"
nav_order: 34
render_with_liquid: false
---

# Module 34 — NoSQL

Two NoSQL stores that cover the most common non-relational patterns:
**MongoDB** — flexible document storage where the schema lives in the application;
**Redis** — in-memory data structures used for caching, session management, queues, and pub/sub.

---

## MongoDB

### Document Model vs Relational Model

```
  Relational (JPA)                    MongoDB (Spring Data)
  ─────────────────────────────────── ──────────────────────────────────────
  Table "products"                    Collection "products"
  Row                                 Document (BSON/JSON object)
  Column with fixed type              Field with flexible type
  Foreign key → JOIN                  Embedded array (no JOIN needed)
  Schema enforced by DB               Schema enforced by application

  authors Table       books Table      Single "authors" Document
  ┌────┬──────┐       ┌────┬──────────┐  {
  │ id │ name │       │ id │ author_id│    "_id": ObjectId("..."),
  ├────┼──────┤       ├────┼──────────┤    "name": "J.K. Rowling",
  │  1 │ JKR  │       │  1 │    1     │    "books": [
  └────┴──────┘       │  2 │    1     │      { "title": "HP1" },
                      └────┴──────────┘      { "title": "HP2" }
                                           ]
                                         }
```

### Entity Annotations

```java
@Document("products")            // maps to MongoDB collection "products"
public class Product {

    @Id                          // maps to MongoDB's _id field (String = ObjectId)
    private String id;

    @Indexed                     // creates a MongoDB index for fast lookups
    private String category;

    // CRITICAL: store BigDecimal as Decimal128, not String.
    // Without targetType, Spring Data serializes BigDecimal as "99.99"
    // (a JSON string) — numeric operators ($lt, $multiply) don't work on strings.
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    @Field("in_stock")           // stored as "in_stock" in the document (not "inStock")
    private boolean inStock;

    private List<String> tags;   // embedded array — no join table, no FK
}
```

---

### Repository — Derived Queries

```
  Method name → MongoDB JSON query
  ─────────────────────────────────────────────────────────────────────
  findByName(name)
    → { "name": "..." }

  findByCategory(cat)
    → { "category": "..." }

  findByPriceLessThan(price)
    → { "price": { "$lt": { "$numberDecimal": "99.99" } } }

  findByInStock(true)
    → { "in_stock": true }       ← uses the @Field name

  findByTagsContaining("java")
    → { "tags": "java" }         ← MongoDB array contains

  findAllByOrderByPriceAsc()
    → { } sort: { "price": 1 }

  findByCategoryOrderByPriceAsc("Books")
    → { "category": "Books" } sort: { "price": 1 }
```

### Repository — @Query JSON Filters

```java
// $all — document must contain ALL listed tags:
@Query("{ 'tags': { '$all': ?0 } }")
List<Product> findByAllTags(List<String> tags);

// Compound filter: category AND in_stock field:
@Query("{ 'category': ?0, 'in_stock': true }")
List<Product> findAvailableByCategory(String category);

// Range query with Decimal128 parameters:
// @Query with BigDecimal serializes as plain string → Decimal128-vs-String mismatch.
// Pass org.bson.types.Decimal128 for the correct $numberDecimal encoding.
@Query("{ 'price': { '$gte': ?0, '$lte': ?1 } }")
List<Product> findByPriceRange(Decimal128 min, Decimal128 max);
// Usage: repo.findByPriceRange(new Decimal128(new BigDecimal("500")), new Decimal128(...))
```

---

### Aggregation Pipeline

```
  Input collection
       │
       ▼  $match  — filter documents (like WHERE)
       │
       ▼  $group  — group + accumulate: COUNT, SUM, AVG, MAX, MIN
       │
       ▼  $sort   — order the grouped results
       │
       ▼  $limit  — take the first N results
       │
       ▼  $project — reshape: rename fields, add computed expressions
       │
     Result documents  (mapped to a Java record or class)
```

```java
// Count + average per category:
mongoTemplate.aggregate(
    newAggregation(
        group("category")
            .count().as("count")
            .avg("price").as("avgPrice"),
        sort(Sort.Direction.DESC, "count")
    ),
    Product.class,
    CategoryStats.class);   // Spring Data maps "_id" → id, "count" → count, …

// Match first, then group (only in-stock items):
newAggregation(
    match(Criteria.where("in_stock").is(true)),  // @Field name!
    group("category").sum("price").as("total")
)

// Computed projection field:
newAggregation(
    match(Criteria.where("category").is("Books")),
    project("name")
        .andExpression("price * 1.2").as("priceWithTax")
)

// Top-N: sort then limit, output raw Document:
newAggregation(
    sort(Sort.Direction.DESC, "price"),
    limit(2),
    project("name", "price")
)
```

**Result type:** use a Java record or class with fields matching the aggregation output field names. MongoDB's `_id` field maps to a Java field named `id`.

---

### BigDecimal ↔ Decimal128 Gotcha

```
  Symptom            Result is empty even though matching documents exist.
  ─────────────────────────────────────────────────────────────────────────────
  Root cause         BigDecimal → serialized as JSON string "99.99" in the query.
                     MongoDB: { "price": { "$lt": "99.99" } }
                     Decimal128 field compared with String = no match.

  Fix 1 (write)      @Field(targetType = FieldType.DECIMAL128) on the entity field.
                     Ensures data is stored as Decimal128, not as a string.

  Fix 2 (query)      Derived methods (findByPriceLessThan, findByPriceGreaterThan)
                     use the entity-aware path → emit $numberDecimal correctly.

  Fix 3 (@Query)     Pass org.bson.types.Decimal128 as the method parameter type
                     instead of BigDecimal → native BSON type, encoded correctly.

  Known limitation   Between keyword and MongoTemplate Criteria still serialize
                     BigDecimal as String — use two separate derived criteria or
                     native Decimal128 parameters for range queries.
```

---

## Redis

Redis is an in-memory data structure store.  Unlike MongoDB (which persists to disk by default), Redis keeps all data in RAM — reads/writes happen in microseconds.

### Data Structures

```
  ┌────────────────┬──────────────────────────────────────────────────────────┐
  │  Structure     │  Use cases                                               │
  ├────────────────┼──────────────────────────────────────────────────────────┤
  │  String        │  Counters, rate limits, session tokens, cached HTML       │
  │  Hash          │  User profiles, session objects, structured records       │
  │  List          │  Task queues (LPUSH + RPOP), activity feeds, recent items │
  │  Set           │  Unique visitors, tag sets, mutual friends, deduplication │
  │  Sorted Set    │  Leaderboards, priority queues, time-series events        │
  │  TTL on any key│  Expiring sessions, OTP codes, cache invalidation         │
  └────────────────┴──────────────────────────────────────────────────────────┘
```

### String — Value Operations

```java
ValueOperations<String, String> values = redis.opsForValue();

values.set("user:42:name", "Alice");
values.get("user:42:name");                         // "Alice"

// Set with TTL — key deleted automatically after duration:
values.set("otp:1234", "987654", Duration.ofMinutes(5));

// Atomic increment (thread-safe counter):
values.set("counter", "0");
redis.opsForValue().increment("counter");            // "1"
redis.opsForValue().increment("counter");            // "2"
```

### Hash — Field Map per Key

```java
HashOperations<String, String, String> hash = redis.opsForHash();

// HSET — set individual fields:
hash.put("user:1", "name", "Alice");
hash.put("user:1", "email", "alice@example.com");

// HMSET — set multiple fields at once:
hash.putAll("user:1", Map.of("name", "Alice", "age", "30"));

// HGET:
String name = hash.get("user:1", "name");

// HGETALL — returns Map<field, value>:
Map<String, String> all = hash.entries("user:1");

// HDEL:
hash.delete("user:1", "age");
```

### List — Ordered Sequence

```java
ListOperations<String, String> list = redis.opsForList();

// Queue (FIFO): enqueue at tail, dequeue from head:
list.rightPush("jobs", "job-1");    // RPUSH
list.rightPush("jobs", "job-2");
list.leftPop("jobs");               // LPOP → "job-1"

// Stack (LIFO): push and pop from head:
list.leftPush("stack", "item-1");   // LPUSH
list.leftPop("stack");              // LPOP → "item-1"

// LRANGE — slice by index (0-based, -1 = last):
list.range("stack", 0, -1);         // all elements

// LLEN:
list.size("jobs");
```

### Set — Unique Members

```java
SetOperations<String, String> set = redis.opsForSet();

set.add("visited:user:1", "page-1", "page-2", "page-1");  // deduped → 2 members
set.isMember("visited:user:1", "page-1");                  // true
set.members("visited:user:1");                             // Set<String>
set.size("visited:user:1");                                // 2L
```

### TTL — Key Expiry

```java
redis.expire("session:abc", Duration.ofMinutes(30));   // set TTL
Long ttl = redis.getExpire("session:abc", SECONDS);     // remaining seconds
redis.persist("session:abc");                          // remove TTL (make permanent)
```

---

### Spring Data Redis Setup

```java
// pom.xml:
// spring-boot-starter-data-redis  ← includes Lettuce client

// Auto-configured beans (no extra config needed for defaults):
// StringRedisTemplate   — String keys and values
// RedisTemplate<K, V>   — typed; values serialized via Jackson or JDK serialization

// Connection properties (application.properties):
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

### Testing Infrastructure

```
  MongoDB tests — @DataMongoTest
  ────────────────────────────────────────────────────────────────────────
  Add de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x to test scope.
  Spring Boot auto-configures an in-process MongoDB (no Docker, no server).
  Set the version property:
    de.flapdoodle.mongodb.embedded.version=6.0.6  (in test/resources/application.properties)

  Redis tests — @SpringBootTest + Testcontainers
  ────────────────────────────────────────────────────────────────────────
  Requires Docker to be running.
  @Testcontainers(disabledWithoutDocker = true) skips gracefully when Docker
  is not available.
  @DynamicPropertySource wires the container's random port into Spring's config.

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
      registry.add("spring.data.redis.host", REDIS::getHost);
      registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }
```

---

## Module 34 — What Was Built

```
  module-34-nosql/
  ├── pom.xml     (Spring Boot 3.3.5, data-mongodb, data-redis, flapdoodle 4.12.2,
  │               spring-boot-testcontainers, testcontainers-junit-jupiter)
  └── src/
      ├── main/java/com/javatraining/nosql/
      │   ├── NoSqlApplication.java            — @SpringBootApplication
      │   ├── document/
      │   │   └── Product.java                 — @Document, @Field(DECIMAL128), embedded tags[]
      │   └── repository/
      │       └── ProductRepository.java       — MongoRepository: derived queries,
      │                                          @Query JSON filters, Decimal128 params
      └── test/java/com/javatraining/nosql/
          ├── MongoRepositoryTest.java   11 tests — CRUD, derived queries, @Query,
          │                                        Decimal128 range, @Field mapping
          ├── MongoAggregationTest.java   6 tests — $group count/sum/avg, $match+$group,
          │                                        $project computed fields, $sort+$limit
          ├── RedisStringHashTest.java    8 tests — String set/get/setex/incr,
          │                                        Hash put/get/entries/delete
          │                                        (skipped without Docker)
          └── RedisListSetTest.java       9 tests — List push/pop/range/size,
                                                   Set add/members/isMember,
                                                   TTL expire/getExpire
                                                   (skipped without Docker)
```

MongoDB tests: **17 passing**.
Redis tests: **17 skipped** (require Docker; pass when Docker is running).

---

## Key Takeaways

```
  @Document          — maps class to MongoDB collection; no schema migration
  @Id (String)       — maps to MongoDB ObjectId; auto-generated
  @Field             — custom field name in the stored document
  @Field(DECIMAL128) — REQUIRED for BigDecimal; default serializes as String
  Embedded arrays    — model one-to-many without JOINs (tags inside document)
  @Indexed           — creates a collection index for faster queries

  Derived queries    — same syntax as JPA; Spring generates the JSON query
  @Query             — explicit MongoDB JSON filter; use Decimal128 params for numeric
  MongoTemplate      — lower-level API for aggregations and custom queries

  Aggregation stages — $match (filter), $group (accumulate), $sort, $limit, $project
  Result types       — Java records or classes; _id → id field mapping

  Redis String       — SET/GET/SETEX for counters, tokens, cached values
  Redis Hash         — HSET/HGETALL for structured objects (user profiles, sessions)
  Redis List         — LPUSH+RPOP = queue; LPUSH+LPOP = stack
  Redis Set          — SADD/SMEMBERS for unique member tracking, tag unions
  TTL                — EXPIRE makes any key self-destruct; foundation of all caching
```
