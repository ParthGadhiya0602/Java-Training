package com.javatraining.jdbc;

import com.javatraining.jdbc.core.DatabaseInitializer;
import com.javatraining.jdbc.model.Order;
import com.javatraining.jdbc.model.Product;
import com.javatraining.jdbc.repository.OrderRepository;
import com.javatraining.jdbc.repository.ProductRepository;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies JDBC transaction management:
 * <ul>
 *   <li>Successful commit makes all changes permanent</li>
 *   <li>Rollback reverts all changes on failure</li>
 *   <li>Savepoints enable partial rollback within a transaction</li>
 *   <li>{@code autoCommit} is always restored after a transaction, even on failure</li>
 * </ul>
 *
 * <p>Uses a dedicated H2 database ({@code jdbc:h2:mem:jdbctx}) so this class
 * runs independently from {@link JdbcCoreTest} and {@link BatchTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionTest {

    private Connection conn;
    private ProductRepository productRepo;
    private OrderRepository   orderRepo;
    private int               productId;   // fresh product inserted in @BeforeEach

    private static final BigDecimal UNIT_PRICE = new BigDecimal("25.00");

    @BeforeAll
    void setUpDatabase() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:jdbctx;DB_CLOSE_DELAY=-1", "sa", "");
        DatabaseInitializer.initialize(conn);
        productRepo = new ProductRepository(conn);
        orderRepo   = new OrderRepository(conn);
    }

    @AfterAll
    void tearDown() throws Exception {
        DatabaseInitializer.dropAll(conn);
        conn.close();
    }

    @BeforeEach
    void seedProduct() throws Exception {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM orders");
            stmt.execute("DELETE FROM products");
        }
        // Fresh product with 100 units of stock
        productId = productRepo.insert(Product.of("Test Product", 25.00, 100));
    }

    // ── Successful commit ────────────────────────────────────────────────────

    @Test
    void successful_order_deducts_stock_atomically() throws Exception {
        int orderId = orderRepo.placeOrder(productId, 5, UNIT_PRICE);
        assertTrue(orderId > 0);
        assertEquals(95, productRepo.findById(productId).orElseThrow().stockQty());
    }

    @Test
    void order_total_price_is_unit_price_times_quantity() throws Exception {
        int orderId = orderRepo.placeOrder(productId, 3, UNIT_PRICE);
        Order order = orderRepo.findById(orderId).orElseThrow();
        assertEquals(new BigDecimal("75.00"), order.totalPrice());
    }

    @Test
    void order_status_is_confirmed_after_placement() throws Exception {
        int orderId = orderRepo.placeOrder(productId, 1, UNIT_PRICE);
        assertEquals("CONFIRMED", orderRepo.findById(orderId).orElseThrow().status());
    }

    @Test
    void multiple_sequential_orders_reduce_stock_cumulatively() throws Exception {
        orderRepo.placeOrder(productId, 10, UNIT_PRICE);
        orderRepo.placeOrder(productId, 15, UNIT_PRICE);
        assertEquals(75, productRepo.findById(productId).orElseThrow().stockQty());
    }

    // ── Rollback on insufficient stock ───────────────────────────────────────

    @Test
    void insufficient_stock_throws_and_leaves_stock_unchanged() throws Exception {
        assertThrows(SQLException.class,
                () -> orderRepo.placeOrder(productId, 200, UNIT_PRICE));
        // Both the stock deduction and any partial inserts must be reverted
        assertEquals(100, productRepo.findById(productId).orElseThrow().stockQty());
    }

    // ── autoCommit restore ───────────────────────────────────────────────────

    @Test
    void autocommit_is_restored_after_successful_transaction() throws Exception {
        assertTrue(conn.getAutoCommit(), "Pre-condition: should start true");
        orderRepo.placeOrder(productId, 1, UNIT_PRICE);
        assertTrue(conn.getAutoCommit(), "Must be restored to true after commit");
    }

    @Test
    void autocommit_is_restored_after_failed_transaction() throws Exception {
        assertTrue(conn.getAutoCommit());
        assertThrows(SQLException.class,
                () -> orderRepo.placeOrder(productId, 999, UNIT_PRICE));
        assertTrue(conn.getAutoCommit(), "Must be restored to true after rollback");
    }

    // ── Savepoints ───────────────────────────────────────────────────────────

    /**
     * Demonstrates partial rollback:
     * <ol>
     *   <li>Delete product A  (before savepoint)</li>
     *   <li>Set savepoint</li>
     *   <li>Delete product B  (after savepoint)</li>
     *   <li>Rollback to savepoint → B is restored; A stays deleted</li>
     *   <li>Commit</li>
     * </ol>
     */
    @Test
    void savepoint_allows_partial_rollback() throws Exception {
        int productBId = productRepo.insert(Product.of("Product B", 15.00, 50));

        conn.setAutoCommit(false);
        try {
            productRepo.delete(productId);                            // before savepoint

            Savepoint sp = conn.setSavepoint("afterDeleteFirst");

            productRepo.delete(productBId);                          // after savepoint

            conn.rollback(sp);  // undo delete of B; delete of A stands
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }

        assertTrue(productRepo.findById(productId).isEmpty(),  "A should be deleted");
        assertTrue(productRepo.findById(productBId).isPresent(), "B should be restored");
    }

    // ── Manual rollback ──────────────────────────────────────────────────────

    @Test
    void manual_rollback_reverts_delete_entirely() throws Exception {
        conn.setAutoCommit(false);
        try {
            productRepo.delete(productId);
            assertTrue(productRepo.findById(productId).isEmpty(),
                    "Row invisible to current connection within transaction");
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }
        assertTrue(productRepo.findById(productId).isPresent(),
                "Row visible again after rollback");
    }
}
