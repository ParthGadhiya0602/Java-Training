package com.javatraining.migration;

import liquibase.Liquibase;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Liquibase applies all changeSets from the master changelog.
 *
 * <p>This test class overrides the default application properties to:
 * <ul>
 *   <li>Disable Flyway - avoids conflict with Flyway migrations on the same DB</li>
 *   <li>Enable Liquibase with the master changelog</li>
 *   <li>Use a separate H2 URL (liquibasedb) so Flyway's schema_history table
 *       and Liquibase's DATABASECHANGELOG table never collide</li>
 * </ul>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml",
        "spring.datasource.url=jdbc:h2:mem:liquibasedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class LiquibaseMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SpringLiquibase springLiquibase;

    // ── Schema structure ──────────────────────────────────────────────────────

    @Test
    void products_table_exists_after_changeSet_001() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void description_column_exists_after_changeSet_003() {
        // NULL is the default for the new nullable column
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE description IS NULL", Integer.class);
        assertThat(count).isNotNull();
    }

    // ── Seed data (changeSet 002) ─────────────────────────────────────────────

    @Test
    void seed_inserts_three_products() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void macbook_exists_with_correct_price() {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT name, price FROM products WHERE name = ?", "MacBook Pro");
        assertThat(row.get("NAME")).isEqualTo("MacBook Pro");
        assertThat(row.get("PRICE").toString()).isEqualTo("2499.00");
    }

    @Test
    void standing_desk_is_out_of_stock() {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT in_stock FROM products WHERE name = ?", "Standing Desk");
        assertThat(row.get("IN_STOCK")).isEqualTo(false);
    }

    // ── Liquibase change log history ──────────────────────────────────────────

    @Test
    void three_changeSets_recorded_in_databasechangelog() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class);
        assertThat(count).isEqualTo(3);
    }
}
