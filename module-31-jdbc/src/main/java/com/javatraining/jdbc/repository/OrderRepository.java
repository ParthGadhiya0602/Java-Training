package com.javatraining.jdbc.repository;

import com.javatraining.jdbc.model.Order;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

/**
 * Demonstrates JDBC transaction management through a real business scenario:
 * placing a product order must atomically deduct stock AND record the order.
 *
 * <p><strong>Transaction pattern used in {@link #placeOrder}:</strong>
 * <pre>
 *   boolean autoCommit = conn.getAutoCommit();
 *   conn.setAutoCommit(false);                 // begin transaction
 *   try {
 *       // ... one or more SQL statements ...
 *       conn.commit();                          // make permanent
 *   } catch (SQLException e) {
 *       conn.rollback();                        // undo all changes
 *       throw e;
 *   } finally {
 *       conn.setAutoCommit(autoCommit);         // restore caller's setting
 *   }
 * </pre>
 *
 * <p>Restoring {@code autoCommit} in {@code finally} is critical when the
 * connection comes from a pool - the next borrower must not inherit an open
 * transaction.
 */
public class OrderRepository {

    private final Connection connection;

    public OrderRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Places an order atomically:
     * <ol>
     *   <li>Deducts {@code quantity} from {@code product.stock_qty} (only if sufficient stock)</li>
     *   <li>Inserts an order record with status {@code CONFIRMED}</li>
     *   <li>Commits both changes - or rolls back entirely on any failure</li>
     * </ol>
     *
     * <p>The stock check uses an atomic {@code UPDATE ... WHERE stock_qty >= ?} rather
     * than a separate SELECT + UPDATE, which avoids the lost-update race condition
     * under concurrent requests.
     *
     * @return the generated order id
     * @throws SQLException if stock is insufficient or any DB error occurs; all changes reverted
     */
    public int placeOrder(int productId, int quantity, BigDecimal unitPrice) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            // Atomic check-and-deduct: row is updated only when stock_qty >= quantity
            String deductSql = """
                    UPDATE products
                       SET stock_qty = stock_qty - ?
                     WHERE id = ? AND stock_qty >= ?
                    """;
            try (PreparedStatement ps = connection.prepareStatement(deductSql)) {
                ps.setInt(1, quantity);
                ps.setInt(2, productId);
                ps.setInt(3, quantity);
                if (ps.executeUpdate() == 0) {
                    connection.rollback();
                    throw new SQLException("Insufficient stock for product id=" + productId);
                }
            }

            // Record the confirmed order
            String insertSql = """
                    INSERT INTO orders (product_id, quantity, total_price, status)
                    VALUES (?, ?, ?, 'CONFIRMED')
                    """;
            int orderId;
            try (PreparedStatement ps = connection.prepareStatement(
                    insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, productId);
                ps.setInt(2, quantity);
                ps.setBigDecimal(3, unitPrice.multiply(BigDecimal.valueOf(quantity)));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) orderId = keys.getInt(1);
                    else throw new SQLException("No generated key for order");
                }
            }

            connection.commit();
            return orderId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);   // always restore for pool safety
        }
    }

    /** Returns the order with the given id, or {@link Optional#empty()} if not found. */
    public Optional<Order> findById(int id) throws SQLException {
        String sql = """
                SELECT id, product_id, quantity, total_price, status
                  FROM orders WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Order(
                            rs.getInt("id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("total_price"),
                            rs.getString("status")
                    ));
                }
                return Optional.empty();
            }
        }
    }
}
