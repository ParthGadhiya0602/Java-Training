package com.javatraining.jdbc;

import com.javatraining.jdbc.core.DatabaseInitializer;
import com.javatraining.jdbc.core.HikariConnectionPool;
import com.javatraining.jdbc.model.Product;
import com.javatraining.jdbc.repository.ProductRepository;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies HikariCP connection pool behaviour:
 * <ul>
 *   <li>Connections are valid and executable</li>
 *   <li>Active/total counts are tracked correctly</li>
 *   <li>Connections return to the pool after {@code close()}</li>
 *   <li>Multiple connections can be borrowed simultaneously</li>
 *   <li>Data persists across connections (same underlying database)</li>
 * </ul>
 *
 * <p>Uses an isolated H2 database ({@code jdbc:h2:mem:jdbcpool}) with
 * {@code DB_CLOSE_ON_EXIT=FALSE} to prevent H2 from closing the database
 * when the last connection is released back to the pool.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionPoolTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:jdbcpool;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private HikariConnectionPool pool;

    @BeforeAll
    void setUpPool() throws Exception {
        pool = new HikariConnectionPool(JDBC_URL, "sa", "");
        try (Connection conn = pool.getConnection()) {
            DatabaseInitializer.initialize(conn);
        }
    }

    @AfterAll
    void tearDownPool() throws Exception {
        try (Connection conn = pool.getConnection()) {
            DatabaseInitializer.dropAll(conn);
        }
        pool.close();
    }

    @BeforeEach
    void clearData() throws Exception {
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM products");
        }
    }

    // ── Basic connectivity ───────────────────────────────────────────────────

    @Test
    void pool_provides_a_non_null_open_connection() throws Exception {
        try (Connection conn = pool.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void connection_from_pool_can_execute_queries() throws Exception {
        try (Connection conn   = pool.getConnection();
             Statement  stmt   = conn.createStatement();
             ResultSet  rs     = stmt.executeQuery("SELECT 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    // ── Pool metrics ─────────────────────────────────────────────────────────

    @Test
    void active_connections_increment_while_borrowed() throws Exception {
        Connection conn = pool.getConnection();
        try {
            assertEquals(1, pool.getActiveConnections());
        } finally {
            conn.close();
        }
    }

    @Test
    void active_connections_drop_to_zero_after_connection_close() throws Exception {
        Connection conn = pool.getConnection();
        conn.close();
        assertEquals(0, pool.getActiveConnections());
    }

    @Test
    void total_connections_is_positive_after_first_borrow() throws Exception {
        try (Connection conn = pool.getConnection()) {
            assertTrue(pool.getTotalConnections() >= 1);
        }
    }

    @Test
    void multiple_connections_can_be_borrowed_simultaneously() throws Exception {
        Connection c1 = pool.getConnection();
        Connection c2 = pool.getConnection();
        try {
            assertEquals(2, pool.getActiveConnections());
            assertFalse(c1.isClosed());
            assertFalse(c2.isClosed());
        } finally {
            c1.close();
            c2.close();
        }
    }

    // ── Data persistence across connections ──────────────────────────────────

    @Test
    void data_persists_across_different_pool_connections() throws Exception {
        int id;
        // Borrow + release connection 1: insert a product
        try (Connection conn = pool.getConnection()) {
            id = new ProductRepository(conn).insert(Product.of("Pool Item", 19.99, 5));
        }
        // Borrow connection 2 (may be a different physical connection): read back
        try (Connection conn = pool.getConnection()) {
            assertTrue(new ProductRepository(conn).findById(id).isPresent());
        }
    }

    @Test
    void returned_connection_can_be_borrowed_and_used_again() throws Exception {
        // First borrow — insert
        try (Connection c = pool.getConnection()) {
            new ProductRepository(c).insert(Product.of("Reuse Test", 5.00, 1));
        }
        // Second borrow — count
        try (Connection c = pool.getConnection()) {
            assertEquals(1, new ProductRepository(c).count());
        }
    }

    // ── Pool lifecycle ───────────────────────────────────────────────────────

    @Test
    void pool_is_not_closed_before_explicit_shutdown() {
        assertFalse(pool.isClosed());
    }
}
