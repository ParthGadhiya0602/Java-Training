package com.javatraining.springtesting.testcontainers;

import com.javatraining.springtesting.model.Product;
import com.javatraining.springtesting.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest + Testcontainers — JPA slice against a real PostgreSQL container.
 *
 * Why use Testcontainers when @DataJpaTest already works with H2?
 *   - H2 SQL dialect differs from PostgreSQL: window functions, JSON operators,
 *     RETURNING clause, and custom types behave differently or are unsupported.
 *   - Indexes, constraints, and sequences work the same as production.
 *   - Schema migrations (Flyway/Liquibase) can be tested against a real engine.
 *
 * @AutoConfigureTestDatabase(replace = NONE):
 *   Prevents @DataJpaTest from replacing the datasource with an embedded database.
 *   The datasource now comes from the Testcontainers connection details.
 *
 * @ServiceConnection (Spring Boot 3.1+):
 *   Reads the container's host, port, database name, user, and password and
 *   auto-registers them as DataSource connection details — no manual
 *   @DynamicPropertySource needed for standard containers.
 *
 * Static @Container:
 *   The container starts once before the first test and stops after the last.
 *   All tests in the class share the same PostgreSQL instance.
 *   @DataJpaTest wraps each test in a rolled-back transaction, so data is clean per test.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ProductRepositoryTCTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired ProductRepository productRepository;

    @BeforeEach
    void cleanup() {
        productRepository.deleteAll();
    }

    @Test
    void save_and_find_persists_to_postgres() {
        Product saved = productRepository.save(Product.builder()
                .name("Laptop")
                .category("Electronics")
                .price(new BigDecimal("999.00"))
                .build());

        assertThat(saved.getId()).isNotNull();

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Laptop");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    void findByCategory_works_on_postgres() {
        productRepository.save(Product.builder().name("Laptop").category("Electronics").price(BigDecimal.TEN).build());
        productRepository.save(Product.builder().name("Keyboard").category("Accessories").price(BigDecimal.TEN).build());

        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(1)
                .first()
                .extracting(Product::getName)
                .isEqualTo("Laptop");
    }

    @Test
    void multiple_saves_produce_unique_auto_incremented_ids() {
        Product p1 = productRepository.save(Product.builder().name("P1").category("X").price(BigDecimal.ONE).build());
        Product p2 = productRepository.save(Product.builder().name("P2").category("X").price(BigDecimal.ONE).build());
        Product p3 = productRepository.save(Product.builder().name("P3").category("X").price(BigDecimal.ONE).build());

        // PostgreSQL IDENTITY column generates strictly increasing IDs
        assertThat(List.of(p1.getId(), p2.getId(), p3.getId()))
                .doesNotHaveDuplicates()
                .allSatisfy(id -> assertThat(id).isPositive());
    }
}
