package com.javatraining.jdbc.repository;

import com.javatraining.jdbc.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD repository for {@link Product} using raw JDBC.
 *
 * <p>Every public method uses {@link PreparedStatement} - never raw
 * {@link Statement} with string concatenation - to prevent SQL injection
 * and enable query-plan caching in the database engine.
 *
 * <p>Resources ({@code PreparedStatement}, {@code ResultSet}) are always
 * closed via try-with-resources, which also closes on exception, preventing
 * connection leaks.
 *
 * <p>The repository does not manage transactions; it operates within whatever
 * transaction the caller has set on the {@link Connection}.  This allows
 * {@link OrderRepository} to enlist multiple repository operations in a single
 * atomic unit.
 */
public class ProductRepository {

    private final Connection connection;

    public ProductRepository(Connection connection) {
        this.connection = connection;
    }

    /**
     * Inserts a product and returns its database-generated id.
     *
     * <p>{@link Statement#RETURN_GENERATED_KEYS} instructs the driver to expose
     * the auto-increment value via {@link PreparedStatement#getGeneratedKeys()}.
     */
    public int insert(Product product) throws SQLException {
        String sql = "INSERT INTO products (name, price, stock_qty) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.name());
            ps.setBigDecimal(2, product.price());
            ps.setInt(3, product.stockQty());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Insert succeeded but no generated key returned");
            }
        }
    }

    /** Returns the product with the given id, or {@link Optional#empty()} if not found. */
    public Optional<Product> findById(int id) throws SQLException {
        String sql = "SELECT id, name, price, stock_qty FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Returns all products ordered by id (insertion order). */
    public List<Product> findAll() throws SQLException {
        String sql = "SELECT id, name, price, stock_qty FROM products ORDER BY id";
        List<Product> products = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) products.add(mapRow(rs));
        }
        return products;
    }

    /** Returns the number of rows in the products table - efficient COUNT(*). */
    public int count() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM products");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Updates the stock quantity for the given product id.
     *
     * @return {@code true} if the row was found and updated; {@code false} if id is unknown
     */
    public boolean updateStock(int id, int newQty) throws SQLException {
        String sql = "UPDATE products SET stock_qty = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes the product with the given id.
     *
     * @return {@code true} if the row was found and deleted; {@code false} otherwise
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── private helper ────────────────────────────────────────────────────────

    /** Maps the current ResultSet row to a Product.  Column access by name
     *  is safer than by index - resilient to SELECT column reordering. */
    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getBigDecimal("price"),
                rs.getInt("stock_qty")
        );
    }
}
