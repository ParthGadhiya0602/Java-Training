package com.javatraining.jdbc.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Thin wrapper around {@link DriverManager} - the simplest way to obtain a JDBC
 * connection.
 *
 * <p><strong>DriverManager</strong> opens a brand-new physical connection on every
 * call; there is no pooling.  It is fine for scripts, tools, or low-traffic apps,
 * but unsuitable for servers (each call pays TCP + authentication overhead).
 * For production use {@link HikariConnectionPool} instead.
 *
 * <p>JDBC drivers register themselves via {@code java.sql.Driver} service-loader
 * (META-INF/services).  No {@code Class.forName()} call is needed since JDBC 4.0.
 *
 * <pre>
 *   ConnectionFactory factory = new ConnectionFactory(
 *       "jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1", "sa", "");
 *   try (Connection conn = factory.getConnection()) {
 *       // use conn - auto-closed by try-with-resources
 *   }
 * </pre>
 */
public class ConnectionFactory {

    private final String url;
    private final String user;
    private final String password;

    public ConnectionFactory(String url, String user, String password) {
        this.url      = url;
        this.user     = user;
        this.password = password;
    }

    /**
     * Opens a new physical connection to the database.
     * Caller is responsible for closing it (use try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public String getUrl() { return url; }
}
