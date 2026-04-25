package com.javatraining.jdbc.core;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Production-grade connection pool backed by HikariCP.
 *
 * <p><strong>Why pooling?</strong>  Opening a raw TCP connection + authenticating
 * to a database takes 5–50 ms.  Under load that overhead dominates.  A pool
 * creates connections once at startup, then lends and recycles them - your app
 * pays microseconds per borrow instead.
 *
 * <p><strong>Key HikariCP configuration knobs:</strong>
 * <pre>
 *   maximumPoolSize  - hard cap on physical connections (match DB max_connections)
 *   minimumIdle      - warm connections kept ready at all times
 *   connectionTimeout - how long to wait for a free connection before throwing
 *   idleTimeout      - time before an idle connection is closed
 *   maxLifetime      - absolute cap on a connection's age (prevents server-side drops)
 * </pre>
 *
 * <p>Implements {@link AutoCloseable} so it can be used in try-with-resources
 * and in {@code @AfterAll} teardown methods.
 */
public class HikariConnectionPool implements AutoCloseable {

    private final HikariDataSource dataSource;

    public HikariConnectionPool(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(3_000);   // 3 s - throw if no connection available
        config.setIdleTimeout(600_000);        // 10 min idle before closing
        config.setMaxLifetime(1_800_000);      // 30 min max lifetime
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Borrows a connection from the pool.
     * The caller MUST close it (try-with-resources) to return it to the pool.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Number of connections currently in use by callers. */
    public int getActiveConnections() {
        HikariPoolMXBean bean = dataSource.getHikariPoolMXBean();
        return bean != null ? bean.getActiveConnections() : 0;
    }

    /** Total physical connections the pool is managing (active + idle). */
    public int getTotalConnections() {
        HikariPoolMXBean bean = dataSource.getHikariPoolMXBean();
        return bean != null ? bean.getTotalConnections() : 0;
    }

    /** Connections sitting idle, ready to be borrowed. */
    public int getIdleConnections() {
        HikariPoolMXBean bean = dataSource.getHikariPoolMXBean();
        return bean != null ? bean.getIdleConnections() : 0;
    }

    /** {@code true} after {@link #close()} is called. */
    public boolean isClosed() {
        return dataSource.isClosed();
    }

    /** Closes all pooled connections and releases resources. */
    @Override
    public void close() {
        dataSource.close();
    }
}
