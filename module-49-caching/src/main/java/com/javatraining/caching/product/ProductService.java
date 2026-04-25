package com.javatraining.caching.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        log.info("Loading product {} from database", id);
        return productRepository.findById(id).orElse(null);
    }

    @Cacheable("productList")
    public List<Product> findAll() {
        log.info("Loading all products from database");
        return productRepository.findAll();
    }

    /**
     * @CachePut always executes the method and stores the result in the cache,
     * so a subsequent @Cacheable call for the same key skips the database entirely.
     *
     * @CacheEvict with allEntries=true invalidates the list cache because the
     * full list is now stale.
     */
    @Caching(
        put    = @CachePut(value = "products", key = "#result.id"),
        evict  = @CacheEvict(value = "productList", allEntries = true)
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
}
