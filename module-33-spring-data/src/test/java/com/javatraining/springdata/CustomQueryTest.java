package com.javatraining.springdata;

import com.javatraining.springdata.entity.Department;
import com.javatraining.springdata.entity.Employee;
import com.javatraining.springdata.repository.DepartmentRepository;
import com.javatraining.springdata.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.javatraining.springdata.config.JpaAuditingConfig;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code @Query} (JPQL and native) and {@code @Modifying} queries.
 *
 * <p>Key differences from derived queries:
 * <ul>
 *   <li>JPQL {@code @Query} - full control over the HQL/JPQL string; references
 *       entity/field names (not table/column names)</li>
 *   <li>Native {@code @Query} - raw SQL; useful for DB-specific syntax or
 *       functions not available in JPQL</li>
 *   <li>{@code @Modifying} - required for UPDATE/DELETE; clears persistence context
 *       by default to avoid stale state</li>
 * </ul>
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class CustomQueryTest {

    @Autowired TestEntityManager    tem;
    @Autowired EmployeeRepository   employees;
    @Autowired DepartmentRepository departments;

    private Department engineering;
    private Department hr;

    @BeforeEach
    void seed() {
        engineering = tem.persist(new Department("Engineering"));
        hr          = tem.persist(new Department("HR"));

        Employee alice = new Employee("Alice",  "alice@test.com",  new BigDecimal("95000"));
        Employee bob   = new Employee("Bob",    "bob@test.com",    new BigDecimal("75000"));
        Employee carol = new Employee("Carol",  "carol@test.com",  new BigDecimal("50000"));
        Employee dave  = new Employee("Dave",   "dave@test.com",   new BigDecimal("40000"));

        carol.setActive(false);
        dave.setActive(false);

        engineering.addEmployee(alice);
        engineering.addEmployee(bob);
        hr.addEmployee(carol);
        hr.addEmployee(dave);

        tem.persist(alice);
        tem.persist(bob);
        tem.persist(carol);
        tem.persist(dave);
        tem.flush();
    }

    // ── @Query JPQL ──────────────────────────────────────────────────────────

    @Test
    void findHighEarners_returns_employees_above_threshold() {
        List<Employee> result = employees.findHighEarners(new BigDecimal("74000"));
        assertThat(result).hasSize(2)
                .extracting(Employee::getName)
                .containsExactly("Alice", "Bob"); // ORDER BY salary DESC
    }

    @Test
    void findHighEarners_returns_empty_when_no_one_qualifies() {
        List<Employee> result = employees.findHighEarners(new BigDecimal("200000"));
        assertThat(result).isEmpty();
    }

    @Test
    void findByDepartmentNameFetched_returns_employees_with_department_loaded() {
        List<Employee> result = employees.findByDepartmentNameFetched("Engineering");
        assertThat(result).hasSize(2);
        // Department is JOIN FETCH'd - accessing it should NOT trigger a lazy load
        result.forEach(e ->
            assertThat(e.getDepartment().getName()).isEqualTo("Engineering")
        );
    }

    // ── @Query native SQL ────────────────────────────────────────────────────

    @Test
    void findHighEarnersNative_returns_same_results_as_jpql() {
        List<Employee> jpql   = employees.findHighEarners(new BigDecimal("74000"));
        List<Employee> native_ = employees.findHighEarnersNative(new BigDecimal("74000"));
        assertThat(native_).extracting(Employee::getName)
                .containsExactlyElementsOf(
                        jpql.stream().map(Employee::getName).toList());
    }

    // ── @Modifying - bulk UPDATE ─────────────────────────────────────────────

    @Test
    void deactivateByDepartmentId_updates_all_employees_in_department() {
        int updated = employees.deactivateByDepartmentId(engineering.getId());
        assertThat(updated).isEqualTo(2); // Alice + Bob

        tem.flush();
        tem.clear();

        List<Employee> engEmployees = employees.findByDepartmentName("Engineering");
        assertThat(engEmployees).allMatch(e -> !e.isActive());
    }

    @Test
    void deactivateByDepartmentId_returns_zero_for_unknown_department() {
        int updated = employees.deactivateByDepartmentId(999L);
        assertThat(updated).isEqualTo(0);
    }

    // ── @Modifying - bulk DELETE ─────────────────────────────────────────────

    @Test
    void deleteInactiveBelow_removes_qualifying_employees() {
        // carol (inactive, 50k) and dave (inactive, 40k) both qualify
        int deleted = employees.deleteInactiveBelow(new BigDecimal("60000"));
        assertThat(deleted).isEqualTo(2);

        tem.flush();
        tem.clear();

        assertThat(employees.findAll()).hasSize(2) // alice + bob remain
                .extracting(Employee::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void deleteInactiveBelow_does_not_touch_active_employees() {
        // alice (active, 95k) should never be deleted even if threshold is high
        employees.deleteInactiveBelow(new BigDecimal("100000"));
        tem.flush();
        tem.clear();

        assertThat(employees.findByEmail("alice@test.com")).isPresent();
    }
}
