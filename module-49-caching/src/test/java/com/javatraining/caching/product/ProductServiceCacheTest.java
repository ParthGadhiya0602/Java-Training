package com.javatraining.caching.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * spring.cache.type=simple (test/application.properties) selects ConcurrentMapCacheManager,
 * which provides the same @Cacheable/@CacheEvict/@CachePut semantics as RedisCacheManager
 * without requiring a running Redis broker.
 *
 * @SpyBean wraps the real ProductRepository so verify() can assert how many times
 * the underlying database was actually called.
 */
@SpringBootTest
class ProductServiceCacheTest {

    @Autowired ProductService productService;
    @SpyBean  ProductRepository productRepository;
    @Autowired CacheManager cacheManager;

    private Product saved;

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
        Product updated = new Product(saved.getId(), "Gadget", new BigDecimal("19.99"));
        productService.save(updated);
        clearInvocations(productRepository);

        Product result = productService.findById(saved.getId());

        verify(productRepository, never()).findById(saved.getId());
        assertThat(result.getName()).isEqualTo("Gadget");
    }

    @Test
    void save_evicts_list_cache_so_next_findAll_hits_repository() {
        productService.findAll();

        productService.save(new Product(null, "New Item", new BigDecimal("5.00")));

        productService.findAll();
        verify(productRepository, times(2)).findAll();
    }
}
