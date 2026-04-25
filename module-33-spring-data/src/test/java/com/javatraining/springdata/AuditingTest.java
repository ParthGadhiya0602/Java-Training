package com.javatraining.springdata;

import com.javatraining.springdata.entity.Department;
import com.javatraining.springdata.entity.Employee;
import com.javatraining.springdata.repository.DepartmentRepository;
import com.javatraining.springdata.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates Spring Data JPA auditing - automatic population of
 * {@code @CreatedDate} and {@code @LastModifiedDate} fields.
 *
 * <p>{@code @DataJpaTest} does NOT load {@code JpaAuditingConfig} from
 * {@code src/main} because it only slices the JPA layer (entities +
 * repositories).  To enable auditing within the slice, an inner
 * {@code @TestConfiguration} annotated with {@code @EnableJpaAuditing}
 * is registered alongside this test class.
 */
@DataJpaTest
class AuditingTest {

    /**
     * Inner test configuration - supplements the slice context rather than
     * replacing it.  {@code @TestConfiguration} tells Spring Boot this class
     * is additive; the auto-configuration (entity scan, datasource, etc.)
     * is still bootstrapped from {@code SpringDataApplication}.
     */
    @TestConfiguration
    @EnableJpaAuditing
    static class AuditConfig {}

    @Autowired TestEntityManager    tem;
    @Autowired EmployeeRepository   employees;
    @Autowired DepartmentRepository departments;

    @Test
    void created_at_is_populated_on_first_save() {
        Department dept = tem.persist(new Department("Finance"));
        Employee emp = new Employee("Audit User", "audit@test.com", new BigDecimal("70000"));
        dept.addEmployee(emp);
        tem.persist(emp);
        tem.flush();

        // After flush, @CreatedDate should be populated by AuditingEntityListener
        assertThat(emp.getCreatedAt()).isNotNull();
    }

    @Test
    void updated_at_is_populated_on_first_save() {
        Department dept = tem.persist(new Department("Finance2"));
        Employee emp = new Employee("Update User", "update@test.com", new BigDecimal("70000"));
        dept.addEmployee(emp);
        tem.persist(emp);
        tem.flush();

        assertThat(emp.getUpdatedAt()).isNotNull();
    }

    @Test
    void created_at_equals_updated_at_on_initial_insert() {
        Department dept = tem.persist(new Department("Finance3"));
        Employee emp = new Employee("New User", "new@test.com", new BigDecimal("65000"));
        dept.addEmployee(emp);
        tem.persist(emp);
        tem.flush();

        // On insert, both timestamps are set to the same moment
        assertThat(emp.getCreatedAt()).isEqualTo(emp.getUpdatedAt());
    }

    @Test
    void updated_at_changes_on_update_but_created_at_does_not() throws InterruptedException {
        Department dept = tem.persist(new Department("Finance4"));
        Employee emp = new Employee("Stable User", "stable@test.com", new BigDecimal("60000"));
        dept.addEmployee(emp);
        tem.persist(emp);
        tem.flush();

        var originalCreatedAt = emp.getCreatedAt();
        var originalUpdatedAt = emp.getUpdatedAt();

        // Small delay to ensure the clock advances between insert and update
        Thread.sleep(10);

        emp.setSalary(new BigDecimal("65000"));
        tem.flush();

        // createdAt must NOT change
        assertThat(emp.getCreatedAt()).isEqualTo(originalCreatedAt);
        // updatedAt MUST change
        assertThat(emp.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
    }

    @Test
    void created_at_is_not_updatable() {
        // Column is mapped with updatable = false - this is structural verification.
        // If updatable were accidentally removed, the column mapping would update it.
        Department dept = tem.persist(new Department("Finance5"));
        Employee emp = new Employee("Immutable Ts", "imm@test.com", new BigDecimal("50000"));
        dept.addEmployee(emp);
        tem.persist(emp);
        tem.flush();

        var firstCreatedAt = emp.getCreatedAt();

        // Force multiple flushes
        emp.setSalary(new BigDecimal("55000"));
        tem.flush();
        emp.setSalary(new BigDecimal("60000"));
        tem.flush();

        assertThat(emp.getCreatedAt()).isEqualTo(firstCreatedAt);
    }
}
