package com.javatraining.springtesting.slice;

import com.javatraining.springtesting.model.Product;
import com.javatraining.springtesting.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest — JPA slice.
 *
 * Loads: JPA repositories, entity classes, TestEntityManager.
 * Does NOT load: @Service, @Controller, @Component (only JPA-related beans).
 * Database: H2 in-memory (auto-configured by @DataJpaTest, replaces the H2 in application.properties).
 * Transaction: each test runs in a transaction that is rolled back after — DB is always clean.
 *
 * TestEntityManager wraps EntityManager for test use:
 *   persist()  — saves and flushes to SQL (without going through the repository interface)
 *   flush()    — writes pending SQL to DB (stays in transaction, not committed yet)
 *   clear()    — evicts the first-level cache so findById() hits the DB instead of returning the cached object
 */
@DataJpaTest
class ProductRepositoryH2Test {

    @Autowired ProductRepository productRepository;
    @Autowired TestEntityManager entityManager;

    @Test
    void save_and_find_by_id() {
        Product product = Product.builder()
                .name("Laptop")
                .category("Electronics")
                .price(new BigDecimal("999.00"))
                .build();

        // persist + flush writes the INSERT; clear evicts first-level cache
        // so the subsequent findById() actually issues a SELECT, not returning the cached object
        entityManager.persistAndFlush(product);
        entityManager.clear();

        Product found = productRepository.findById(product.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Laptop");
        assertThat(found.isActive()).isTrue();  // @Builder.Default active = true
    }

    @Test
    void findByCategory_returns_only_matching_products() {
        entityManager.persist(Product.builder().name("Laptop").category("Electronics").price(BigDecimal.TEN).build());
        entityManager.persist(Product.builder().name("Phone").category("Electronics").price(BigDecimal.TEN).build());
        entityManager.persist(Product.builder().name("Desk").category("Furniture").price(BigDecimal.TEN).build());
        entityManager.flush();

        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(2)
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Phone");
    }

    @Test
    void findByActiveTrue_excludes_inactive_products() {
        entityManager.persist(Product.builder().name("Active")  .category("X").price(BigDecimal.TEN).active(true).build());
        entityManager.persist(Product.builder().name("Inactive").category("X").price(BigDecimal.TEN).active(false).build());
        entityManager.flush();

        List<Product> active = productRepository.findByActiveTrue();

        assertThat(active).hasSize(1)
                .extracting(Product::getName)
                .containsExactly("Active");
    }

    @Test
    void id_is_auto_generated_on_save() {
        Product p1 = productRepository.save(
                Product.builder().name("A").category("X").price(BigDecimal.ONE).build());
        Product p2 = productRepository.save(
                Product.builder().name("B").category("X").price(BigDecimal.ONE).build());

        assertThat(p1.getId()).isNotNull();
        assertThat(p2.getId()).isNotNull();
        assertThat(p1.getId()).isNotEqualTo(p2.getId());
    }
}
