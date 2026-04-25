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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates pagination and sorting with Spring Data JPA.
 *
 * <h2>Pagination</h2>
 * {@link PageRequest#of(int, int)} creates a {@link org.springframework.data.domain.Pageable}
 * that Spring Data translates to {@code LIMIT} / {@code OFFSET} SQL.
 *
 * <p>The returned {@link Page} object carries:
 * <ul>
 *   <li>{@code getContent()} - entities on this page</li>
 *   <li>{@code getTotalElements()} - total count (extra COUNT query)</li>
 *   <li>{@code getTotalPages()}</li>
 *   <li>{@code isFirst()} / {@code isLast()} / {@code hasNext()}</li>
 * </ul>
 *
 * <h2>Sorting</h2>
 * {@link Sort#by(String)} or {@link Sort#by(Sort.Direction, String)} embedded in
 * the Pageable, or passed as a standalone parameter to repository methods.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PaginationSortingTest {

    @Autowired TestEntityManager    tem;
    @Autowired EmployeeRepository   employees;
    @Autowired DepartmentRepository departments;

    @BeforeEach
    void seed() {
        Department eng = tem.persist(new Department("Engineering"));

        // 8 employees with different salaries; all active
        String[][] data = {
            {"Alice",   "alice@page.com",   "90000"},
            {"Bob",     "bob@page.com",     "80000"},
            {"Carol",   "carol@page.com",   "70000"},
            {"Dave",    "dave@page.com",    "60000"},
            {"Eve",     "eve@page.com",     "50000"},
            {"Frank",   "frank@page.com",   "45000"},
            {"Grace",   "grace@page.com",   "40000"},
            {"Henry",   "henry@page.com",   "35000"},
        };
        for (String[] row : data) {
            Employee e = new Employee(row[0], row[1], new BigDecimal(row[2]));
            eng.addEmployee(e);
            tem.persist(e);
        }
        tem.flush();
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Test
    void first_page_returns_correct_size() {
        Page<Employee> page = employees.findByActiveTrue(PageRequest.of(0, 3));
        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    void page_metadata_is_correct() {
        Page<Employee> page = employees.findByActiveTrue(PageRequest.of(0, 3));
        assertThat(page.getTotalElements()).isEqualTo(8L);
        assertThat(page.getTotalPages()).isEqualTo(3);   // ceil(8/3) = 3
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void last_page_contains_remaining_elements() {
        Page<Employee> last = employees.findByActiveTrue(PageRequest.of(2, 3));
        // 8 total, 3 per page: page 0 has 3, page 1 has 3, page 2 has 2
        assertThat(last.getContent()).hasSize(2);
        assertThat(last.isLast()).isTrue();
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void empty_page_beyond_range_returns_empty_content() {
        Page<Employee> beyond = employees.findByActiveTrue(PageRequest.of(99, 3));
        assertThat(beyond.getContent()).isEmpty();
        assertThat(beyond.getTotalElements()).isEqualTo(8L); // total unchanged
    }

    // ── Sorting ───────────────────────────────────────────────────────────────

    @Test
    void page_sorted_by_name_ascending_orders_alphabetically() {
        Page<Employee> page = employees.findByActiveTrue(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.ASC, "name")));
        List<String> names = page.getContent().stream()
                .map(Employee::getName).toList();
        assertThat(names).isSortedAccordingTo(String::compareTo);
    }

    @Test
    void page_sorted_by_salary_descending_highest_first() {
        Page<Employee> page = employees.findByActiveTrue(
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "salary")));
        List<BigDecimal> salaries = page.getContent().stream()
                .map(Employee::getSalary).toList();
        // each salary should be >= the next
        for (int i = 0; i < salaries.size() - 1; i++) {
            assertThat(salaries.get(i)).isGreaterThanOrEqualTo(salaries.get(i + 1));
        }
    }

    @Test
    void pagination_with_salary_filter_pages_correctly() {
        // employees earning > 50000: Alice(90k), Bob(80k), Carol(70k), Dave(60k) = 4
        Page<Employee> page = employees.findBySalaryGreaterThan(
                new BigDecimal("50000"), PageRequest.of(0, 2, Sort.by("salary").descending()));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(4L);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void multi_field_sort_is_stable() {
        // Sort by active ASC (all same), then name ASC - tests multi-property Sort
        Page<Employee> page = employees.findByActiveTrue(
                PageRequest.of(0, 8,
                        Sort.by(Sort.Direction.ASC, "active")
                            .and(Sort.by(Sort.Direction.ASC, "name"))));
        List<String> names = page.getContent().stream()
                .map(Employee::getName).toList();
        assertThat(names).isSortedAccordingTo(String::compareTo);
    }
}
