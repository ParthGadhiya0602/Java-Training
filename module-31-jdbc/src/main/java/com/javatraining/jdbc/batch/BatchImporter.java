package com.javatraining.jdbc.batch;

import com.javatraining.jdbc.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates JDBC batch processing: grouping multiple DML statements into a
 * single network round-trip instead of N separate calls.
 *
 * <p><strong>Without batch</strong> — N round trips:
 * <pre>
 *   for each row:
 *       ps.executeUpdate();   ← network + DB parse + DB execute
 * </pre>
 *
 * <p><strong>With batch</strong> — 1 round trip:
 * <pre>
 *   for each row:
 *       ps.addBatch();        ← just queues parameters locally
 *   ps.executeBatch();        ← one network call for all rows
 * </pre>
 *
 * <p>For large imports the difference is dramatic: 10 000 rows that took 10 s
 * with individual inserts often complete in under 1 s with a single batch.
 *
 * <p>The returned {@code int[]} contains one entry per batched statement.
 * Each value is the update count (rows affected) for that statement, or
 * {@link java.sql.Statement#SUCCESS_NO_INFO} ({@code -2}) when the driver
 * cannot determine the count.
 */
public class BatchImporter {

    private final Connection connection;

    public BatchImporter(Connection connection) {
        this.connection = connection;
    }

    /**
     * Inserts all products in a single batch.
     *
     * @return array of row counts, one per product (each is 1 for a successful insert)
     */
    public int[] insertBatch(List<Product> products) throws SQLException {
        if (products.isEmpty()) return new int[0];

        String sql = "INSERT INTO products (name, price, stock_qty) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Product p : products) {
                ps.setString(1, p.name());
                ps.setBigDecimal(2, p.price());
                ps.setInt(3, p.stockQty());
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    /**
     * Updates prices for a map of {@code id → newPrice} in a single batch.
     *
     * @return array of row counts, one per entry in the map
     */
    public int[] updatePricesBatch(Map<Integer, BigDecimal> priceUpdates) throws SQLException {
        if (priceUpdates.isEmpty()) return new int[0];

        String sql = "UPDATE products SET price = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Map.Entry<Integer, BigDecimal> entry : priceUpdates.entrySet()) {
                ps.setBigDecimal(1, entry.getValue());
                ps.setInt(2, entry.getKey());
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }
}
