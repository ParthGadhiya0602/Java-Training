package com.javatraining.integration;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed ProductRepository.
 * Tested against a real PostgreSQL instance managed by Testcontainers.
 *
 * Design notes:
 *  - Takes a {@link Connection} directly so tests can control transactions.
 *  - SQLExceptions are wrapped in {@link RuntimeException} to keep the interface clean.
 *  - Uses PostgreSQL {@code RETURNING id} to retrieve auto-generated keys inline.
 */
public class JdbcProductRepository implements ProductRepository {

    /** DDL to create the products table if it does not already exist. */
    public static final String CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS products (
            id       BIGSERIAL    PRIMARY KEY,
            name     VARCHAR(255) NOT NULL,
            price    DECIMAL(10,2) NOT NULL,
            category VARCHAR(100)
        )
        """;

    private final Connection conn;

    public JdbcProductRepository(Connection conn) {
        this.conn = conn;
    }

    @Override
    public Product save(Product product) {
        try {
            if (product.id() == 0) return insert(product);
            return update(product);
        } catch (SQLException e) {
            throw new RuntimeException("save failed", e);
        }
    }

    private Product insert(Product p) throws SQLException {
        String sql = "INSERT INTO products (name, price, category) VALUES (?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.name());
            ps.setBigDecimal(2, BigDecimal.valueOf(p.price()));
            ps.setString(3, p.category());
            try (ResultSet rs = ps.executeQuery()) {   // RETURNING → executeQuery
                rs.next();
                return new Product(rs.getLong("id"), p.name(), p.price(), p.category());
            }
        }
    }

    private Product update(Product p) throws SQLException {
        String sql = "UPDATE products SET name=?, price=?, category=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.name());
            ps.setBigDecimal(2, BigDecimal.valueOf(p.price()));
            ps.setString(3, p.category());
            ps.setLong(4, p.id());
            ps.executeUpdate();
            return p;
        }
    }

    @Override
    public Optional<Product> findById(long id) {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    @Override
    public List<Product> findAll() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM products ORDER BY id")) {
            List<Product> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("findAll failed", e);
        }
    }

    @Override
    public List<Product> findByCategory(String category) {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM products WHERE category = ? ORDER BY id")) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByCategory failed", e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("deleteById failed", e);
        }
    }

    @Override
    public int count() {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM products")) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("count failed", e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getBigDecimal("price").doubleValue(),
            rs.getString("category")
        );
    }
}
