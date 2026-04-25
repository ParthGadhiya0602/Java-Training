package com.javatraining.observability.product;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit tests — no Spring context.
 * SimpleMeterRegistry keeps all meter state in memory, making it ideal for
 * verifying metric instrumentation without starting a full application.
 */
class ProductServiceTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ProductRepository   productRepository = mock(ProductRepository.class);
    private final ProductService      productService    = new ProductService(productRepository, meterRegistry);

    @Test
    void lookup_counter_increments_on_each_findById_call() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        productService.findById(1L);
        productService.findById(2L);
        productService.findById(3L);

        assertThat(meterRegistry.counter("products.lookups").count()).isEqualTo(3.0);
    }

    @Test
    void lookup_timer_records_one_observation_per_findById_call() {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        productService.findById(1L);
        productService.findById(2L);

        assertThat(meterRegistry.timer("products.lookup.duration").count()).isEqualTo(2L);
    }

    @Test
    void active_gauge_reflects_number_of_products_saved() {
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return new Product(99L, p.getName(), p.getPrice(), p.getCategory());
        });

        productService.save(new Product(null, "Widget", BigDecimal.ONE,  "Tools"));
        productService.save(new Product(null, "Gadget", BigDecimal.TEN,  "Electronics"));

        assertThat(meterRegistry.get("products.active").gauge().value()).isEqualTo(2.0);
    }
}
