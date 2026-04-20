package com.javatraining.jdbc;

import com.javatraining.jdbc.core.DatabaseInitializer;
import com.javatraining.jdbc.model.Product;
import com.javatraining.jdbc.repository.ProductRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies core JDBC operations: DriverManager, PreparedStatement, ResultSet,
 * generated keys, SQL injection prevention, and BigDecimal precision.
 *
 * <p>Each test class uses its own named H2 in-memory database
 * ({@code jdbc:h2:mem:jdbccore}) to guarantee isolation from sibling test classes.
 * {@code DB_CLOSE_DELAY=-1} keeps the database alive for the duration of the
 * test run (it would otherwise be dropped when the last connection closes).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JdbcCoreTest {

    private Connection conn;
    private ProductRepository repo;

    @BeforeAll
    void setUpDatabase() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:jdbccore;DB_CLOSE_DELAY=-1", "sa", "");
        DatabaseInitializer.initialize(conn);
        repo = new ProductRepository(conn);
    }

    @AfterAll
    void tearDown() throws Exception {
        DatabaseInitializer.dropAll(conn);
        conn.close();
    }

    @BeforeEach
    void clearData() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM products");
        }
    }

    // ── INSERT ───────────────────────────────────────────────────────────────

    @Test
    void insert_returns_positive_generated_id() throws Exception {
        int id = repo.insert(Product.of("Widget", 9.99, 10));
        assertTrue(id > 0, "Generated id should be positive");
    }

    @Test
    void inserted_product_has_correct_fields() throws Exception {
        Product p = new Product("Gadget", new BigDecimal("49.99"), 25);
        int id = repo.insert(p);
        Product found = repo.findById(id).orElseThrow();
        assertEquals("Gadget", found.name());
        assertEquals(new BigDecimal("49.99"), found.price());
        assertEquals(25, found.stockQty());
    }

    @Test
    void each_insert_gets_a_unique_id() throws Exception {
        int id1 = repo.insert(Product.of("A", 1.00, 1));
        int id2 = repo.insert(Product.of("B", 2.00, 2));
        assertNotEquals(id1, id2);
    }

    // ── SELECT ───────────────────────────────────────────────────────────────

    @Test
    void find_by_id_returns_correct_product() throws Exception {
        int id = repo.insert(Product.of("Keyboard", 79.99, 50));
        Optional<Product> found = repo.findById(id);
        assertTrue(found.isPresent());
        assertEquals("Keyboard", found.get().name());
    }

    @Test
    void find_by_id_returns_empty_for_unknown_id() throws Exception {
        assertTrue(repo.findById(999).isEmpty());
    }

    @Test
    void find_all_returns_empty_list_when_table_is_empty() throws Exception {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void find_all_returns_all_products_in_id_order() throws Exception {
        repo.insert(Product.of("First",  1.00, 1));
        repo.insert(Product.of("Second", 2.00, 2));
        repo.insert(Product.of("Third",  3.00, 3));
        List<Product> all = repo.findAll();
        assertEquals(3, all.size());
        assertEquals("First",  all.get(0).name());
        assertEquals("Second", all.get(1).name());
        assertEquals("Third",  all.get(2).name());
    }

    @Test
    void count_returns_correct_row_count() throws Exception {
        repo.insert(Product.of("X", 1.00, 1));
        repo.insert(Product.of("Y", 2.00, 2));
        assertEquals(2, repo.count());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void update_stock_changes_quantity() throws Exception {
        int id = repo.insert(Product.of("Monitor", 299.99, 10));
        repo.updateStock(id, 7);
        assertEquals(7, repo.findById(id).orElseThrow().stockQty());
    }

    @Test
    void update_stock_returns_false_for_unknown_id() throws Exception {
        assertFalse(repo.updateStock(999, 5));
    }

    @Test
    void update_does_not_affect_other_products() throws Exception {
        int id1 = repo.insert(Product.of("Item1", 10.00, 100));
        int id2 = repo.insert(Product.of("Item2", 20.00, 200));
        repo.updateStock(id1, 50);
        assertEquals(200, repo.findById(id2).orElseThrow().stockQty());
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void delete_removes_the_product() throws Exception {
        int id = repo.insert(Product.of("Temporary", 5.00, 3));
        assertTrue(repo.delete(id));
        assertTrue(repo.findById(id).isEmpty());
    }

    @Test
    void delete_returns_false_for_unknown_id() throws Exception {
        assertFalse(repo.delete(999));
    }

    // ── SECURITY & PRECISION ─────────────────────────────────────────────────

    @Test
    void prepared_statement_prevents_sql_injection() throws Exception {
        // If name were injected into a raw Statement, the DROP would execute.
        // PreparedStatement treats the entire string as a data value, not SQL.
        String malicious = "'; DROP TABLE products; --";
        int id = repo.insert(Product.of(malicious, 1.00, 1));
        Optional<Product> found = repo.findById(id);
        assertTrue(found.isPresent(), "Table must still exist (not dropped)");
        assertEquals(malicious, found.get().name(), "Stored as literal string");
    }

    @Test
    void bigdecimal_price_survives_round_trip_with_exact_precision() throws Exception {
        // floating-point 0.1 + 0.2 = 0.30000000000000004 — BigDecimal avoids this
        Product p = new Product("Precision Item", new BigDecimal("19.99"), 1);
        int id = repo.insert(p);
        BigDecimal retrieved = repo.findById(id).orElseThrow().price();
        assertEquals(new BigDecimal("19.99"), retrieved);
    }
}
