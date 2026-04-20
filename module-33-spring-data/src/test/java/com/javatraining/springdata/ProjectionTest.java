package com.javatraining.springdata;

import com.javatraining.springdata.entity.Department;
import com.javatraining.springdata.entity.Employee;
import com.javatraining.springdata.projection.EmployeeNameDto;
import com.javatraining.springdata.projection.EmployeeSummary;
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
 * Demonstrates interface projections and DTO projections.
 *
 * <h2>Interface projection</h2>
 * Spring Data creates a JDK dynamic proxy that implements the projection interface.
 * Only the declared getter columns are SELECTed — {@code SELECT name, email} instead
 * of {@code SELECT *}.
 *
 * <h2>DTO projection</h2>
 * JPQL {@code new} expression constructs a record directly in the query.  No proxy
 * overhead; the record is immutable and type-safe.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class ProjectionTest {

    @Autowired TestEntityManager    tem;
    @Autowired EmployeeRepository   employees;
    @Autowired DepartmentRepository departments;

    @BeforeEach
    void seed() {
        Department eng = tem.persist(new Department("Engineering"));

        Employee alice = new Employee("Alice", "alice@proj.com", new BigDecimal("90000"));
        Employee bob   = new Employee("Bob",   "bob@proj.com",   new BigDecimal("70000"));
        Employee carol = new Employee("Carol", "carol@proj.com", new BigDecimal("50000"));
        carol.setActive(false);

        eng.addEmployee(alice);
        eng.addEmployee(bob);
        eng.addEmployee(carol);

        tem.persist(alice);
        tem.persist(bob);
        tem.persist(carol);
        tem.flush();
    }

    // ── Interface projection ──────────────────────────────────────────────────

    @Test
    void interface_projection_returns_only_name_and_email() {
        List<EmployeeSummary> summaries = employees.findByActiveTrue();
        // carol is inactive; only alice and bob
        assertThat(summaries).hasSize(2);
        assertThat(summaries).extracting(EmployeeSummary::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void interface_projection_default_method_composes_fields() {
        List<EmployeeSummary> summaries = employees.findByActiveTrue();
        // default getDisplayName() = getName() + " <" + getEmail() + ">"
        assertThat(summaries).extracting(EmployeeSummary::getDisplayName)
                .containsExactlyInAnyOrder(
                        "Alice <alice@proj.com>",
                        "Bob <bob@proj.com>");
    }

    @Test
    void interface_projection_does_not_include_salary_field() {
        // The projection interface only declares getName() and getEmail().
        // There is no getSalary() method — the salary column is not fetched.
        List<EmployeeSummary> summaries = employees.findByActiveTrue();
        assertThat(summaries).isNotEmpty();
        // If salary were accidentally exposed, compilation would fail — this
        // test documents the intent rather than asserting at runtime.
    }

    // ── DTO projection ────────────────────────────────────────────────────────

    @Test
    void dto_projection_returns_name_and_salary_only() {
        List<EmployeeNameDto> dtos = employees.findActiveSalaries();
        // carol inactive → only alice + bob
        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(EmployeeNameDto::name)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void dto_projection_is_ordered_by_salary_descending() {
        List<EmployeeNameDto> dtos = employees.findActiveSalaries();
        assertThat(dtos).hasSize(2);
        // Alice 90k > Bob 70k
        assertThat(dtos.get(0).name()).isEqualTo("Alice");
        assertThat(dtos.get(1).name()).isEqualTo("Bob");
    }

    @Test
    void dto_projection_salary_values_match_persisted_values() {
        List<EmployeeNameDto> dtos = employees.findActiveSalaries();
        assertThat(dtos).extracting(EmployeeNameDto::salary)
                .containsExactly(
                        new BigDecimal("90000.00"),
                        new BigDecimal("70000.00"));
    }
}
