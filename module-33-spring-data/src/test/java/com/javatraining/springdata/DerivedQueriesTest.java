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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Spring Data derived query methods.
 *
 * <p>Spring Data parses the method name at startup and generates the JPQL.
 * No {@code @Query} annotation needed — the name IS the query.
 *
 * <p>{@code @DataJpaTest} loads only JPA-related beans (entities, repositories,
 * Hibernate, H2), and wraps each test in a transaction that rolls back after
 * the test — no manual cleanup needed.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class DerivedQueriesTest {

    @Autowired TestEntityManager tem;
    @Autowired EmployeeRepository  employees;
    @Autowired DepartmentRepository departments;

    private Department engineering;
    private Department hr;

    @BeforeEach
    void seed() {
        engineering = tem.persist(new Department("Engineering"));
        hr          = tem.persist(new Department("HR"));

        Employee alice = new Employee("Alice Smith",  "alice@test.com",  new BigDecimal("90000"));
        Employee bob   = new Employee("Bob Johnson",  "bob@test.com",    new BigDecimal("70000"));
        Employee carol = new Employee("Carol White",  "carol@test.com",  new BigDecimal("85000"));
        Employee dave  = new Employee("Dave Smith",   "dave@test.com",   new BigDecimal("60000"));
        Employee eve   = new Employee("Eve Brown",    "eve@test.com",    new BigDecimal("55000"));

        engineering.addEmployee(alice);
        engineering.addEmployee(bob);
        engineering.addEmployee(carol);
        hr.addEmployee(dave);
        hr.addEmployee(eve);

        eve.setActive(false);

        tem.persist(alice);
        tem.persist(bob);
        tem.persist(carol);
        tem.persist(dave);
        tem.persist(eve);
        tem.flush();
    }

    @Test
    void findByEmail_returns_single_employee() {
        Optional<Employee> result = employees.findByEmail("alice@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice Smith");
    }

    @Test
    void findByEmail_returns_empty_for_unknown_email() {
        Optional<Employee> result = employees.findByEmail("nobody@test.com");
        assertThat(result).isEmpty();
    }

    @Test
    void findByNameContainingIgnoreCase_matches_partial_name() {
        List<Employee> smiths = employees.findByNameContainingIgnoreCase("smith");
        assertThat(smiths).hasSize(2)
                .extracting(Employee::getName)
                .containsExactlyInAnyOrder("Alice Smith", "Dave Smith");
    }

    @Test
    void findByActive_returns_only_active_employees() {
        List<Employee> active = employees.findByActive(true);
        assertThat(active).hasSize(4).allMatch(Employee::isActive);
    }

    @Test
    void findByActive_returns_only_inactive_employees() {
        List<Employee> inactive = employees.findByActive(false);
        assertThat(inactive).hasSize(1)
                .extracting(Employee::getName)
                .containsExactly("Eve Brown");
    }

    @Test
    void findBySalaryBetween_returns_employees_in_range() {
        List<Employee> result = employees.findBySalaryBetween(
                new BigDecimal("65000"), new BigDecimal("92000"));
        // Alice (90k), Bob (70k), Carol (85k)
        assertThat(result).hasSize(3)
                .extracting(Employee::getName)
                .containsExactlyInAnyOrder("Alice Smith", "Bob Johnson", "Carol White");
    }

    @Test
    void findByDepartment_returns_all_employees_in_dept() {
        List<Employee> result = employees.findByDepartment(engineering);
        assertThat(result).hasSize(3);
    }

    @Test
    void findByDepartmentName_traverses_association() {
        // Spring Data navigates the ManyToOne path: e.department.name = ?
        List<Employee> hrEmployees = employees.findByDepartmentName("HR");
        assertThat(hrEmployees).hasSize(2)
                .extracting(Employee::getName)
                .containsExactlyInAnyOrder("Dave Smith", "Eve Brown");
    }

    @Test
    void countByActive_returns_correct_count() {
        assertThat(employees.countByActive(true)).isEqualTo(4L);
        assertThat(employees.countByActive(false)).isEqualTo(1L);
    }

    @Test
    void existsByName_returns_true_when_department_exists() {
        assertThat(departments.existsByName("Engineering")).isTrue();
        assertThat(departments.existsByName("Marketing")).isFalse();
    }

    @Test
    void findAll_returns_all_persisted_employees() {
        assertThat(employees.findAll()).hasSize(5);
    }

    @Test
    void count_returns_total_number_of_employees() {
        assertThat(employees.count()).isEqualTo(5L);
    }
}
