package com.javatraining.springdata.repository;

import com.javatraining.springdata.entity.Department;
import com.javatraining.springdata.entity.Employee;
import com.javatraining.springdata.projection.EmployeeNameDto;
import com.javatraining.springdata.projection.EmployeeSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Employee} — showcases all major Spring Data JPA features:
 *
 * <ol>
 *   <li><b>Derived queries</b> — method name parsed into JPQL at startup</li>
 *   <li><b>{@code @Query} JPQL</b> — explicit JPQL when derived names become unwieldy</li>
 *   <li><b>{@code @Query} native</b> — raw SQL; useful for DB-specific syntax</li>
 *   <li><b>{@code @Modifying}</b> — bulk UPDATE/DELETE via JPQL</li>
 *   <li><b>Interface projections</b> — Spring proxy returns only selected columns</li>
 *   <li><b>DTO projections</b> — constructor expression in JPQL</li>
 *   <li><b>Pagination</b> — {@link Page} + {@link Pageable}</li>
 *   <li><b>Sorting</b> — {@link org.springframework.data.domain.Sort} parameter</li>
 * </ol>
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // ── Derived queries ───────────────────────────────────────────────────────

    /** SELECT e FROM Employee e WHERE e.email = ?1 */
    Optional<Employee> findByEmail(String email);

    /** SELECT e FROM Employee e WHERE e.name LIKE %?1% */
    List<Employee> findByNameContainingIgnoreCase(String namePart);

    /** SELECT e FROM Employee e WHERE e.active = ?1 */
    List<Employee> findByActive(boolean active);

    /** SELECT e FROM Employee e WHERE e.salary BETWEEN ?1 AND ?2 */
    List<Employee> findBySalaryBetween(BigDecimal min, BigDecimal max);

    /** SELECT e FROM Employee e WHERE e.department = ?1 */
    List<Employee> findByDepartment(Department department);

    /** SELECT e FROM Employee e WHERE e.department.name = ?1 */
    List<Employee> findByDepartmentName(String departmentName);

    /** SELECT COUNT(e) FROM Employee e WHERE e.active = ?1 */
    long countByActive(boolean active);

    // ── @Query — JPQL ─────────────────────────────────────────────────────────

    /**
     * Employees earning above a threshold, ordered by salary descending.
     * Named parameters used instead of positional for readability.
     */
    @Query("SELECT e FROM Employee e WHERE e.salary > :threshold ORDER BY e.salary DESC")
    List<Employee> findHighEarners(@Param("threshold") BigDecimal threshold);

    /**
     * Employees in a department loaded with their department in one JOIN.
     * Avoids N+1 when department name is accessed after the query.
     */
    @Query("SELECT e FROM Employee e JOIN FETCH e.department d WHERE d.name = :deptName")
    List<Employee> findByDepartmentNameFetched(@Param("deptName") String deptName);

    // ── @Query — native SQL ───────────────────────────────────────────────────

    /**
     * Same threshold query expressed in native SQL — demonstrates
     * {@code nativeQuery = true}.
     */
    @Query(value = "SELECT * FROM employees WHERE salary > :threshold ORDER BY salary DESC",
           nativeQuery = true)
    List<Employee> findHighEarnersNative(@Param("threshold") BigDecimal threshold);

    // ── @Modifying — bulk UPDATE / DELETE ────────────────────────────────────

    /**
     * Deactivates all employees in a given department without loading entities.
     * {@code @Modifying} marks the query as a write operation; the caller must
     * be inside a transaction (Spring Data provides one for repository calls by
     * default, but explicit {@code @Transactional} is required on modifying queries).
     */
    @Modifying
    @Query("UPDATE Employee e SET e.active = false WHERE e.department.id = :deptId")
    int deactivateByDepartmentId(@Param("deptId") Long deptId);

    /**
     * Deletes inactive employees whose salary is below the given amount.
     */
    @Modifying
    @Query("DELETE FROM Employee e WHERE e.active = false AND e.salary < :threshold")
    int deleteInactiveBelow(@Param("threshold") BigDecimal threshold);

    // ── Interface projection ──────────────────────────────────────────────────

    /**
     * Returns only name + email columns via a JDK proxy.
     * Spring Data infers the projection type from the return type parameter.
     */
    List<EmployeeSummary> findByActiveTrue();

    // ── DTO projection ────────────────────────────────────────────────────────

    /**
     * {@code new} expression in JPQL constructs the record directly.
     * Only {@code name} and {@code salary} columns are fetched — no SELECT *.
     */
    @Query("SELECT new com.javatraining.springdata.projection.EmployeeNameDto(e.name, e.salary) " +
           "FROM Employee e WHERE e.active = true ORDER BY e.salary DESC")
    List<EmployeeNameDto> findActiveSalaries();

    // ── Pagination ────────────────────────────────────────────────────────────

    /**
     * Returns a single page of employees, sorted and sized by the caller's
     * {@link Pageable} (e.g. {@code PageRequest.of(0, 5, Sort.by("name"))}).
     *
     * <p>The returned {@link Page} contains:
     * <ul>
     *   <li>{@code getContent()} — entities on this page</li>
     *   <li>{@code getTotalElements()} — total count across all pages</li>
     *   <li>{@code getTotalPages()}</li>
     *   <li>{@code hasNext()} / {@code hasPrevious()}</li>
     * </ul>
     */
    Page<Employee> findByActiveTrue(Pageable pageable);

    /** Paginated slice of employees above a salary threshold. */
    Page<Employee> findBySalaryGreaterThan(BigDecimal threshold, Pageable pageable);
}
