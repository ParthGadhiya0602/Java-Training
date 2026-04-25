package com.javatraining.webflux.repository;

import com.javatraining.webflux.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataR2dbcTest - R2DBC repository slice.
 *
 * Loads: R2DBC repositories, ConnectionFactory, R2dbcEntityTemplate.
 * Does NOT load: web layer, services, or any @Component beans.
 * Schema: schema.sql from the classpath is applied by R2dbcInitializationAutoConfiguration.
 *
 * No automatic transaction rollback (unlike @DataJpaTest) - R2DBC does not support
 * the same transactional test isolation. Use @BeforeEach deleteAll() for clean state.
 *
 * .block() is used only in @BeforeEach setup - never in production code.
 * In tests, .block() is acceptable for setup/teardown where you need to wait for completion
 * before the test body runs. The actual assertions use StepVerifier.
 */
@DataR2dbcTest
class ProductRepositoryTest {

    @Autowired ProductRepository repository;

    @BeforeEach
    void cleanup() {
        // block() acceptable in @BeforeEach - ensures cleanup completes before test starts
        repository.deleteAll().block();
    }

    @Test
    void save_assigns_generated_id_and_find_by_id_returns_entity() {
        Product saved = repository.save(
                Product.builder().name("Laptop").category("Electronics").price(new BigDecimal("999.00")).build()
        ).block();

        assertThat(saved.getId()).isNotNull();

        StepVerifier.create(repository.findById(saved.getId()))
                .assertNext(p -> {
                    assertThat(p.getName()).isEqualTo("Laptop");
                    assertThat(p.getCategory()).isEqualTo("Electronics");
                    assertThat(p.isActive()).isTrue();   // @Builder.Default
                })
                .verifyComplete();
    }

    @Test
    void findByCategory_returns_only_matching_products() {
        repository.save(Product.builder().name("Laptop").category("Electronics").price(BigDecimal.TEN).build()).block();
        repository.save(Product.builder().name("Phone").category("Electronics").price(BigDecimal.TEN).build()).block();
        repository.save(Product.builder().name("Desk").category("Furniture").price(BigDecimal.TEN).build()).block();

        StepVerifier.create(repository.findByCategory("Electronics")
                        .map(Product::getName)
                        .sort())   // sort for deterministic order
                .assertNext(name -> assertThat(name).isEqualTo("Laptop"))
                .assertNext(name -> assertThat(name).isEqualTo("Phone"))
                .verifyComplete();
    }

    @Test
    void findByActiveTrue_excludes_inactive_products() {
        repository.save(Product.builder().name("Active").category("X").price(BigDecimal.ONE).active(true).build()).block();
        repository.save(Product.builder().name("Inactive").category("X").price(BigDecimal.ONE).active(false).build()).block();

        StepVerifier.create(repository.findByActiveTrue())
                .assertNext(p -> assertThat(p.getName()).isEqualTo("Active"))
                .verifyComplete();
    }

    @Test
    void multiple_saves_produce_unique_ids() {
        Product p1 = repository.save(Product.builder().name("A").category("X").price(BigDecimal.ONE).build()).block();
        Product p2 = repository.save(Product.builder().name("B").category("X").price(BigDecimal.ONE).build()).block();

        assertThat(p1.getId()).isNotEqualTo(p2.getId());
        assertThat(p1.getId()).isPositive();
        assertThat(p2.getId()).isPositive();
    }
}
