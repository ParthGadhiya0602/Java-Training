package com.javatraining.webflux.service;

import com.javatraining.webflux.dto.ProductRequest;
import com.javatraining.webflux.dto.ProductResponse;
import com.javatraining.webflux.exception.ProductNotFoundException;
import com.javatraining.webflux.model.Product;
import com.javatraining.webflux.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pure unit test — no Spring context, no Netty, no database.
 *
 * StepVerifier — Reactor's test utility for asserting publisher behaviour:
 *   .create(publisher)    — subscribes to the publisher and starts the verification
 *   .assertNext(consumer) — asserts the next emitted element satisfies the consumer
 *   .verifyComplete()     — asserts no more elements and the stream terminates normally
 *   .expectError(Class)   — asserts the stream terminates with the specified exception type
 *   .verify()             — blocks until the expected terminal signal arrives
 *
 * Why StepVerifier instead of .block()?
 *   .block() throws away error signals as unchecked exceptions, cannot assert intermediate
 *   elements, and hides backpressure behaviour. StepVerifier gives precise control over
 *   each signal (onNext, onError, onComplete) in sequence.
 */
class ProductServiceTest {

    // Plain Mockito — no @MockBean, no Spring context startup
    ProductRepository repository = mock(ProductRepository.class);
    ProductService service = new ProductService(repository);

    @Test
    void findAll_emits_all_products_in_order() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(new BigDecimal("999.00")).build();
        Product p2 = Product.builder().id(2L).name("Mouse").category("Accessories").price(new BigDecimal("29.00")).build();
        when(repository.findAll()).thenReturn(Flux.just(p1, p2));

        StepVerifier.create(service.findAll())
                .assertNext(r -> assertThat(r.name()).isEqualTo("Laptop"))
                .assertNext(r -> assertThat(r.name()).isEqualTo("Mouse"))
                .verifyComplete();
    }

    @Test
    void findById_wraps_entity_in_mono_and_maps_to_response() {
        Product product = Product.builder().id(1L).name("Laptop").category("Electronics")
                .price(new BigDecimal("999.00")).build();
        when(repository.findById(1L)).thenReturn(Mono.just(product));

        StepVerifier.create(service.findById(1L))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo(1L);
                    assertThat(r.name()).isEqualTo("Laptop");
                    assertThat(r.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findById_empty_repo_emits_ProductNotFoundException() {
        when(repository.findById(99L)).thenReturn(Mono.empty());

        // expectErrorMatches lets you assert both the type and message
        StepVerifier.create(service.findById(99L))
                .expectErrorMatches(ex ->
                        ex instanceof ProductNotFoundException &&
                        ex.getMessage().contains("99"))
                .verify();
    }

    @Test
    void findAll_empty_flux_completes_without_elements() {
        when(repository.findAll()).thenReturn(Flux.empty());

        // verifyComplete() with no assertNext asserts 0 elements then complete signal
        StepVerifier.create(service.findAll())
                .verifyComplete();
    }

    @Test
    void create_saves_entity_and_returns_response() {
        Product saved = Product.builder().id(10L).name("Keyboard").category("Accessories")
                .price(new BigDecimal("89.00")).build();
        when(repository.save(any(Product.class))).thenReturn(Mono.just(saved));

        ProductRequest request = new ProductRequest("Keyboard", new BigDecimal("89.00"), "Accessories");

        StepVerifier.create(service.create(request))
                .assertNext(r -> {
                    assertThat(r.id()).isEqualTo(10L);
                    assertThat(r.name()).isEqualTo("Keyboard");
                    assertThat(r.price()).isEqualByComparingTo("89.00");
                })
                .verifyComplete();
    }
}
