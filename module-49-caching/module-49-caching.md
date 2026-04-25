---
title: "Module 49 — Caching"
parent: "Phase 6 — Production & Architecture"
nav_order: 49
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-49-caching/src){: .btn .btn-outline }

# Module 49 — Caching

## What this module covers

Spring Cache abstraction with Redis as the production cache store.
Key patterns: cache-aside with `@Cacheable`, write-through with `@CachePut`,
eviction with `@CacheEvict`, and per-cache TTL configuration via
`RedisCacheManagerBuilderCustomizer`.

---

## Project structure

```
src/main/java/com/javatraining/caching/
├── CachingApplication.java
├── config/
│   └── CacheConfig.java          # @EnableCaching + per-cache TTL customizer
└── product/
    ├── Product.java               # @Entity
    ├── ProductRepository.java     # JpaRepository
    └── ProductService.java        # @Cacheable, @CachePut, @CacheEvict, @Caching
```

---

## Spring Cache annotations

### `@Cacheable` — cache-aside read

The method body runs only on a cache miss. On a hit, Spring returns the
cached value without calling the method.

```java
@Cacheable(value = "products", key = "#id")
public Product findById(Long id) {
    return productRepository.findById(id).orElse(null);
}

@Cacheable("productList")
public List<Product> findAll() {
    return productRepository.findAll();
}
```

### `@CachePut` — write-through on save

The method **always executes**, and the result is written to the cache.
A subsequent `@Cacheable` call for the same key skips the database entirely.

```java
@CachePut(value = "products", key = "#result.id")
```

`#result` is a SpEL expression referring to the method's return value.

### `@CacheEvict` — invalidation on write/delete

```java
@CacheEvict(value = "products", key = "#id")
```

`allEntries = true` removes every entry in the named cache (used for list caches
that are wholesale stale after any mutation).

### `@Caching` — combine multiple cache operations on one method

```java
@Caching(
    put   = @CachePut(value = "products", key = "#result.id"),
    evict = @CacheEvict(value = "productList", allEntries = true)
)
public Product save(Product product) {
    return productRepository.save(product);
}

@Caching(evict = {
    @CacheEvict(value = "products", key = "#id"),
    @CacheEvict(value = "productList", allEntries = true)
})
public void deleteById(Long id) {
    productRepository.deleteById(id);
}
```

---

## Redis configuration

`RedisCacheManagerBuilderCustomizer` customizes per-cache TTL and serializer
when Spring Boot autoconfigures `RedisCacheManager`. The customizer is a plain
bean — it does not open a Redis connection on creation.

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        RedisSerializationContext.SerializationPair<Object> jsonPair =
                RedisSerializationContext.SerializationPair.fromSerializer(serializer);

        RedisCacheConfiguration products = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(jsonPair);

        RedisCacheConfiguration productList = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1))
                .disableCachingNullValues()
                .serializeValuesWith(jsonPair);

        return builder -> builder
                .withCacheConfiguration("products", products)
                .withCacheConfiguration("productList", productList);
    }
}
```

`GenericJackson2JsonRedisSerializer` stores values as JSON with embedded type
information, so no `Serializable` marker is needed on cached entities.

### Production properties

```properties
spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

---

## Testing without a Redis broker

`spring.cache.type=simple` selects `ConcurrentMapCacheManager`, which honours
all the same `@Cacheable`/`@CachePut`/`@CacheEvict` semantics as `RedisCacheManager`.
Redis autoconfiguration is excluded so no Lettuce connection is attempted.

```properties
# test/application.properties
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
spring.cache.type=simple
```

Because `RedisCacheManagerBuilderCustomizer` is just a lambda bean with no Redis
dependency, it is created safely and ignored during test context startup.

### Test strategy

`@SpyBean ProductRepository` wraps the real JPA repository. `verify()` counts
how many times the underlying database was actually hit, proving cache hits and
misses without inspecting Redis internals.

```java
@SpringBootTest
class ProductServiceCacheTest {

    @Autowired ProductService productService;
    @SpyBean  ProductRepository productRepository;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager.getCache("products").clear();
        cacheManager.getCache("productList").clear();
        saved = productRepository.save(new Product(null, "Widget", new BigDecimal("9.99")));
        clearInvocations(productRepository);
    }

    @Test
    void findById_served_from_cache_on_second_call() {
        productService.findById(saved.getId());
        productService.findById(saved.getId());
        verify(productRepository, times(1)).findById(saved.getId());
    }

    @Test
    void deleteById_evicts_cache_so_next_find_hits_repository() {
        productService.findById(saved.getId());
        productService.deleteById(saved.getId());
        clearInvocations(productRepository);
        productService.findById(saved.getId());
        verify(productRepository, times(1)).findById(saved.getId());
    }

    @Test
    void save_populates_cache_so_subsequent_find_skips_repository() {
        productService.save(new Product(saved.getId(), "Gadget", new BigDecimal("19.99")));
        clearInvocations(productRepository);
        productService.findById(saved.getId());
        verify(productRepository, never()).findById(saved.getId());
    }

    @Test
    void save_evicts_list_cache_so_next_findAll_hits_repository() {
        productService.findAll();
        productService.save(new Product(null, "New Item", new BigDecimal("5.00")));
        productService.findAll();
        verify(productRepository, times(2)).findAll();
    }
}
```

---

## Tests

| Class                    | Type              | Count |
|--------------------------|-------------------|-------|
| `ProductServiceCacheTest`| `@SpringBootTest` | 4     |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **4/4 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `RedisCacheManagerBuilderCustomizer` instead of a direct `RedisCacheManager` bean | Customizer plugs into Spring Boot's autoconfiguration without breaking test context; no `@ConditionalOnBean` ordering issues |
| `spring.cache.type=simple` in tests | Switches autoconfiguration to `ConcurrentMapCacheManager` with identical annotation semantics — no Docker, no embedded server |
| `@CachePut` with `#result.id` key | Result-based key means the ID is set by the database (auto-increment) and known only after the insert |
| `@Caching` on `save` and `deleteById` | Single annotation site that keeps both the per-item cache and the list cache in sync |
| `disableCachingNullValues()` | Prevents a deleted entity from occupying cache space and masking a later re-insert |
{% endraw %}
