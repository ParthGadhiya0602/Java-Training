package com.javatraining.integration;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link JdbcProductRepository} against a real PostgreSQL
 * instance managed by Testcontainers.
 *
 * What Testcontainers demonstrates:
 *   @Testcontainers            — activates the JUnit 5 lifecycle extension
 *   @Container (static)        — one container shared across all tests; started
 *                                before the first test, stopped after the last
 *   disabledWithoutDocker=true — skips the entire class gracefully when Docker
 *                                is not available (no red X, just "skipped")
 *   PostgreSQLContainer        — typed container: exposes getJdbcUrl(),
 *                                getUsername(), getPassword()
 *   @BeforeEach                — creates a fresh connection and recreates the
 *                                schema for every test (isolation)
 *   @AfterEach                 — closes the connection after every test
 *
 * Integration-testing principles shown:
 *   - Real database: no H2 compatibility surprises (BIGSERIAL, RETURNING, etc.)
 *   - Schema setup in @BeforeEach: every test starts with an empty table
 *   - Container reuse (static): pay the Docker pull/start cost only once per suite
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresProductRepositoryTest {

    /**
     * One PostgreSQL 16 container shared across all tests in this class.
     * Static + @Container: Testcontainers starts it once and stops it after all tests.
     */
    @Container
    static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("tester")
            .withPassword("secret");

    Connection             conn;
    JdbcProductRepository  repository;

    @BeforeEach
    void setUpSchemaAndConnection() throws SQLException {
        conn = DriverManager.getConnection(
            postgres.getJdbcUrl(),
            postgres.getUsername(),
            postgres.getPassword()
        );
        // Recreate table for a clean slate on each test
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS products");
            st.execute(JdbcProductRepository.CREATE_TABLE);
        }
        repository = new JdbcProductRepository(conn);
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) conn.close();
    }

    // ── Save / Find ───────────────────────────────────────────────────────────

    @Test
    void save_assigns_generated_id() {
        Product saved = repository.save(new Product(0, "Widget", 9.99, "gadgets"));

        assertNotEquals(0, saved.id(), "database should assign a positive id");
        assertEquals("Widget", saved.name());
    }

    @Test
    void find_by_id_returns_saved_product() {
        Product saved = repository.save(new Product(0, "Gizmo", 14.99, "gadgets"));

        Optional<Product> found = repository.findById(saved.id());

        assertTrue(found.isPresent());
        assertEquals("Gizmo",   found.get().name());
        assertEquals("gadgets", found.get().category());
        assertEquals(14.99,     found.get().price(), 0.01);
    }

    @Test
    void find_by_id_returns_empty_for_unknown_id() {
        Optional<Product> result = repository.findById(99_999L);
        assertTrue(result.isEmpty());
    }

    // ── Find all / by category ────────────────────────────────────────────────

    @Test
    void find_all_returns_every_saved_product() {
        repository.save(new Product(0, "A", 1.0, "cat"));
        repository.save(new Product(0, "B", 2.0, "cat"));
        repository.save(new Product(0, "C", 3.0, "other"));

        List<Product> all = repository.findAll();

        assertEquals(3, all.size());
    }

    @Test
    void find_all_returns_products_ordered_by_id() {
        repository.save(new Product(0, "A", 1.0, "x"));
        repository.save(new Product(0, "B", 2.0, "x"));
        repository.save(new Product(0, "C", 3.0, "x"));

        List<Product> all = repository.findAll();
        for (int i = 1; i < all.size(); i++) {
            assertTrue(all.get(i).id() > all.get(i - 1).id(),
                "products should be ordered ascending by id");
        }
    }

    @Test
    void find_by_category_returns_only_matching_products() {
        repository.save(new Product(0, "Widget", 9.99, "gadgets"));
        repository.save(new Product(0, "Spanner", 4.99, "tools"));
        repository.save(new Product(0, "Gizmo",  14.99, "gadgets"));

        List<Product> gadgets = repository.findByCategory("gadgets");

        assertEquals(2, gadgets.size());
        assertTrue(gadgets.stream().allMatch(p -> "gadgets".equals(p.category())));
    }

    @Test
    void find_by_category_returns_empty_list_for_unknown_category() {
        repository.save(new Product(0, "Widget", 9.99, "gadgets"));

        List<Product> result = repository.findByCategory("nonexistent");

        assertTrue(result.isEmpty());
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void save_with_existing_id_updates_record() {
        Product original = repository.save(new Product(0, "OldName", 5.0, "cat"));
        Product updated  = repository.save(new Product(original.id(), "NewName", 9.99, "cat"));

        assertEquals("NewName", updated.name());
        assertEquals("NewName", repository.findById(original.id()).get().name());
        assertEquals(1,         repository.count(), "no new row created");
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_removes_product() {
        Product saved = repository.save(new Product(0, "Temp", 1.0, "x"));

        boolean deleted = repository.deleteById(saved.id());

        assertTrue(deleted);
        assertTrue(repository.findById(saved.id()).isEmpty());
    }

    @Test
    void delete_returns_false_for_nonexistent_id() {
        assertFalse(repository.deleteById(99_999L));
    }

    // ── Count ─────────────────────────────────────────────────────────────────

    @Test
    void count_returns_number_of_stored_products() {
        assertEquals(0, repository.count(), "table should be empty after schema reset");

        repository.save(new Product(0, "A", 1.0, "x"));
        repository.save(new Product(0, "B", 2.0, "x"));
        repository.save(new Product(0, "C", 3.0, "x"));

        assertEquals(3, repository.count());
    }

    // ── PostgreSQL-specific behaviour ─────────────────────────────────────────

    @Test
    void ids_from_bigserial_are_unique_and_positive() {
        Product p1 = repository.save(new Product(0, "P1", 1.0, "x"));
        Product p2 = repository.save(new Product(0, "P2", 2.0, "x"));
        Product p3 = repository.save(new Product(0, "P3", 3.0, "x"));

        // BIGSERIAL guarantees positive, unique, increasing ids
        assertTrue(p1.id() > 0);
        assertTrue(p2.id() > p1.id());
        assertTrue(p3.id() > p2.id());
    }

    @Test
    void price_precision_preserved_to_two_decimal_places() {
        Product saved = repository.save(new Product(0, "Item", 12.34, "test"));
        double retrieved = repository.findById(saved.id()).get().price();
        assertEquals(12.34, retrieved, 0.001);
    }
}
