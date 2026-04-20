package com.javatraining.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that Flyway applies all versioned migrations correctly.
 *
 * <p>{@code @SpringBootTest} loads the full application context.
 * Flyway runs automatically on startup (spring.flyway.enabled=true),
 * applying V1 through V6 against the H2 in-memory database.
 *
 * <p>JdbcTemplate queries the migrated schema to verify structure and data.
 * Flyway's own {@link Flyway} bean exposes migration history metadata.
 */
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Flyway flyway;

    // ── Schema structure ──────────────────────────────────────────────────────

    @Test
    void employees_table_has_expected_columns() {
        // If the table doesn't exist this throws — a structural smoke test
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM employees", Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void departments_table_exists_after_v2() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM departments", Integer.class);
        assertThat(count).isNotNull().isGreaterThanOrEqualTo(0);
    }

    @Test
    void employees_has_department_id_column_after_v4() {
        // Query the column — throws if column does not exist
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE department_id IS NOT NULL", Integer.class);
        assertThat(count).isNotNull();
    }

    // ── Seed data (V3) ────────────────────────────────────────────────────────

    @Test
    void seed_inserts_three_departments() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM departments", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void seed_inserts_four_employees() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM employees", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void alice_exists_with_correct_salary() {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT name, salary FROM employees WHERE email = ?", "alice@example.com");
        assertThat(row.get("NAME")).isEqualTo("Alice");
        // H2 returns BigDecimal for DECIMAL columns
        assertThat(row.get("SALARY").toString()).isEqualTo("95000.00");
    }

    // ── Zero-downtime migration (V4 → V5 → V6) ───────────────────────────────

    @Test
    void backfill_assigns_department_to_all_employees() {
        // V5 sets department_id = 1 for all existing employees
        Integer nullCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE department_id IS NULL", Integer.class);
        assertThat(nullCount).isZero();
    }

    @Test
    void all_employees_assigned_to_engineering_after_backfill() {
        // Engineering was inserted first → id = 1
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE department_id = 1", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    // ── Flyway history ────────────────────────────────────────────────────────

    @Test
    void all_six_migrations_applied_successfully() {
        long successCount = flyway.info().applied().length;
        assertThat(successCount).isEqualTo(6);
    }

    @Test
    void migration_versions_are_v1_through_v6() {
        List<String> versions = java.util.Arrays.stream(flyway.info().applied())
                .map(info -> info.getVersion().getVersion())
                .toList();
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6");
    }
}
