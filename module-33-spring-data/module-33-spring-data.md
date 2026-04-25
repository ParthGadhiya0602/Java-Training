---
title: "Module 33 — Spring Data JPA"
nav_order: 33
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-33-spring-data/src){: .btn .btn-outline }

# Module 33 — Spring Data JPA

Spring Data JPA sits on top of JPA/Hibernate and eliminates most boilerplate.
You define an interface that extends `JpaRepository` and Spring generates the
implementation at runtime — no SQL, no JPQL for common queries, no transaction
management code.

---

## Repository Hierarchy

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  Repository<T, ID>              (marker interface)                  │
  │    └── CrudRepository<T, ID>    (save, findById, findAll, delete…)  │
  │          └── PagingAndSortingRepository<T, ID>  (findAll(Pageable)) │
  │                └── JpaRepository<T, ID>                             │
  │                      ├── saveAndFlush, saveAllAndFlush              │
  │                      ├── deleteAllInBatch, deleteInBatch            │
  │                      ├── flush, getById                             │
  │                      └── findAll(Example<S>)  (Query-By-Example)    │
  └─────────────────────────────────────────────────────────────────────┘

  Your interface:
  public interface EmployeeRepository extends JpaRepository<Employee, Long> { … }

  Spring generates a proxy at startup that implements every method.
  Zero lines of persistence code needed for standard CRUD.
```

---

## Derived Query Methods

Spring Data parses the method name at startup and generates JPQL.
No `@Query` annotation needed — the name IS the query.

```
  Method name anatomy:
  ┌──────────┬───────┬─────────────────┬──────────────┬──────────────┐
  │ findBy   │       │  NameContaining │ IgnoreCase   │              │
  │ countBy  │ And   │  SalaryBetween  │              │              │
  │ existsBy │ Or    │  Active         │              │              │
  │ deleteBy │       │  DepartmentName │ (traversal)  │              │
  └──────────┴───────┴─────────────────┴──────────────┴──────────────┘

  Generated JPQL examples:
  findByEmail(email)
    → SELECT e FROM Employee e WHERE e.email = ?1

  findByNameContainingIgnoreCase(name)
    → SELECT e FROM Employee e WHERE LOWER(e.name) LIKE %?1%

  findBySalaryBetween(min, max)
    → SELECT e FROM Employee e WHERE e.salary BETWEEN ?1 AND ?2

  findByDepartmentName(name)           ← traverses ManyToOne association
    → SELECT e FROM Employee e WHERE e.department.name = ?1

  countByActive(active)
    → SELECT COUNT(e) FROM Employee e WHERE e.active = ?1
```

---

## @Query — JPQL and Native SQL

When derived names become unwieldy, or you need full query control:

```java
// JPQL — entity/field names (not table/column):
@Query("SELECT e FROM Employee e WHERE e.salary > :threshold ORDER BY e.salary DESC")
List<Employee> findHighEarners(@Param("threshold") BigDecimal threshold);

// JOIN FETCH — avoids N+1 when you'll access a LAZY association:
@Query("SELECT e FROM Employee e JOIN FETCH e.department d WHERE d.name = :deptName")
List<Employee> findByDepartmentNameFetched(@Param("deptName") String deptName);

// Native SQL — raw SQL; useful for DB-specific syntax:
@Query(value = "SELECT * FROM employees WHERE salary > :threshold ORDER BY salary DESC",
       nativeQuery = true)
List<Employee> findHighEarnersNative(@Param("threshold") BigDecimal threshold);
```

---

## @Modifying — Bulk UPDATE and DELETE

```java
// Bulk UPDATE — skips loading entities into memory:
@Modifying
@Query("UPDATE Employee e SET e.active = false WHERE e.department.id = :deptId")
int deactivateByDepartmentId(@Param("deptId") Long deptId);

// Bulk DELETE — removes rows without loading them:
@Modifying
@Query("DELETE FROM Employee e WHERE e.active = false AND e.salary < :threshold")
int deleteInactiveBelow(@Param("threshold") BigDecimal threshold);
```

```
  Why @Modifying?
  ─────────────────────────────────────────────────────────────────────
  Without it, Spring Data treats the query as a SELECT and wraps it in
  a read-only transaction.  @Modifying switches to a write transaction
  and by default clears the persistence context after execution to
  prevent stale in-memory entities diverging from the updated DB state.
```

---

## Projections — Fetch Only What You Need

Two flavours — interface proxy (no extra class) vs record DTO (type-safe, no proxy).

### Interface Projection

```java
// Declare the interface:
public interface EmployeeSummary {
    String getName();
    String getEmail();

    // Default method — runs in Java, no extra query:
    default String getDisplayName() {
        return getName() + " <" + getEmail() + ">";
    }
}

// Use it as the return type:
List<EmployeeSummary> findByActiveTrue();

// Spring generates: SELECT name, email FROM employees WHERE active = true
// Only the declared columns are fetched — no SELECT *.
```

### DTO Projection (Record)

```java
// Declare a record:
public record EmployeeNameDto(String name, BigDecimal salary) {}

// Use the JPQL 'new' expression:
@Query("SELECT new com.example.EmployeeNameDto(e.name, e.salary) " +
       "FROM Employee e WHERE e.active = true ORDER BY e.salary DESC")
List<EmployeeNameDto> findActiveSalaries();
// Constructs the record directly in the query — no proxy overhead.
```

```
  Comparison:
  ┌─────────────────────┬────────────────────────┬────────────────────────┐
  │                     │  Interface projection  │  DTO projection        │
  ├─────────────────────┼────────────────────────┼────────────────────────┤
  │ Class needed?       │ No (just interface)    │ Yes (record or class)  │
  │ Default methods?    │ Yes                    │ No                     │
  │ Type safety         │ JDK proxy              │ Compile-time           │
  │ JPQL expression     │ Not needed             │ new Dto(fields)        │
  │ Proxy overhead      │ Small                  │ None                   │
  └─────────────────────┴────────────────────────┴────────────────────────┘
```

---

## Pagination and Sorting

```java
// PageRequest.of(page, size)             — 0-indexed page number
// PageRequest.of(page, size, Sort.by())  — with sorting

Page<Employee> page = repo.findByActiveTrue(
    PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "salary")));

page.getContent();          // List<Employee> — entities on this page
page.getTotalElements();    // long — total count across ALL pages
page.getTotalPages();       // int  — ceil(total / pageSize)
page.isFirst();             // boolean
page.isLast();              // boolean
page.hasNext();             // boolean
page.hasPrevious();         // boolean
```

```
  SQL generated (H2 / PostgreSQL):
  ─────────────────────────────────────────────────────────────────────
  SELECT * FROM employees WHERE active = true
    ORDER BY salary DESC
    LIMIT 5 OFFSET 0;                      ← page 0

  SELECT COUNT(e.id) FROM employees e WHERE active = true;
                                           ← extra COUNT for metadata
```

### Sorting standalone

```java
// Sort without pagination:
List<Employee> all = repo.findAll(Sort.by("name").ascending());

// Multi-field:
Sort sort = Sort.by(Direction.ASC, "department.name")
               .and(Sort.by(Direction.DESC, "salary"));
```

---

## Auditing — @CreatedDate / @LastModifiedDate

```java
// 1. Enable auditing:
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}

// 2. Register the listener on the entity:
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Employee {

    @CreatedDate
    @Column(nullable = false, updatable = false)   // never changes after insert
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
```

```
  Lifecycle:
  ─────────────────────────────────────────────────────────────────────
  INSERT  → AuditingEntityListener sets createdAt = now(), updatedAt = now()
  UPDATE  → AuditingEntityListener sets updatedAt = now()
            createdAt is NOT touched (updatable = false)
```

### Auditing in @DataJpaTest

`@DataJpaTest` is a slice — it does NOT load `JpaAuditingConfig` from `src/main`.
Two options to activate auditing inside the slice:

```java
// Option A — import the production config:
@DataJpaTest
@Import(JpaAuditingConfig.class)
class MyTest { … }

// Option B — inner @TestConfiguration (supplements, does not replace):
@DataJpaTest
class AuditingTest {
    @TestConfiguration
    @EnableJpaAuditing
    static class AuditConfig {}
}
```

---

## @DataJpaTest Slice

```
  @DataJpaTest loads:                        @DataJpaTest does NOT load:
  ─────────────────────────────────────────────────────────────────────
  ✓ @Entity classes                          ✗ @Service / @Controller
  ✓ JpaRepository interfaces                 ✗ @Component (unless @Repository)
  ✓ TestEntityManager                        ✗ Security config
  ✓ In-memory H2 by default                 ✗ JpaAuditingConfig (manual import)
  ✓ Transactional (rollback after each test) ✗ Full application context
```

```java
@DataJpaTest
@Import(JpaAuditingConfig.class)
class EmployeeRepositoryTest {

    @Autowired TestEntityManager tem;  // flush/clear without going through repos
    @Autowired EmployeeRepository employees;

    @Test
    void findByEmail_returns_matching_employee() {
        Employee e = tem.persist(new Employee("Alice", "a@t.com", new BigDecimal("70000")));
        tem.flush();

        assertThat(employees.findByEmail("a@t.com"))
            .isPresent()
            .get().extracting(Employee::getName).isEqualTo("Alice");
    }
}
```

---

## Transaction Management

Spring Data repositories are transactional by default:
- `findAll`, `findById`, etc. run in a **read-only** transaction.
- `save`, `delete`, `@Modifying` queries run in a **read-write** transaction.

```java
// Explicit @Transactional for service-layer spanning multiple repo calls:
@Service
public class EmployeeService {

    @Transactional
    public void transferEmployee(Long employeeId, Long newDeptId) {
        Employee emp = employees.findById(employeeId).orElseThrow();
        Department dept = departments.findById(newDeptId).orElseThrow();
        // Both changes committed together or both rolled back.
        emp.setDepartment(dept);
    }
}
```

---

## Module 33 — What Was Built

```
  module-33-spring-data/
  ├── pom.xml           (Spring Boot 3.3.5, spring-boot-starter-data-jpa, H2, starter-test)
  └── src/
      ├── main/java/com/javatraining/springdata/
      │   ├── SpringDataApplication.java   — @SpringBootApplication entry point
      │   ├── entity/
      │   │   ├── Department.java          — @OneToMany(cascade=ALL, orphanRemoval=true)
      │   │   └── Employee.java            — @ManyToOne(LAZY), @CreatedDate, @LastModifiedDate
      │   ├── repository/
      │   │   ├── EmployeeRepository.java  — derived queries, @Query JPQL/native,
      │   │   │                              @Modifying, projections, pagination
      │   │   └── DepartmentRepository.java
      │   ├── projection/
      │   │   ├── EmployeeSummary.java     — interface projection with default method
      │   │   └── EmployeeNameDto.java     — record DTO projection
      │   └── config/
      │       └── JpaAuditingConfig.java   — @EnableJpaAuditing
      └── test/java/com/javatraining/springdata/
          ├── DerivedQueriesTest.java  12 tests — findByEmail, findByName, findBySalaryBetween,
          │                                       findByDepartmentName, countByActive, …
          ├── CustomQueryTest.java      8 tests — @Query JPQL, native SQL, @Modifying
          │                                       UPDATE/DELETE, JOIN FETCH
          ├── ProjectionTest.java       6 tests — interface projection, default method,
          │                                       DTO record projection, ordering
          ├── PaginationSortingTest.java 8 tests — Page metadata, last page, empty beyond range,
          │                                       sort ascending/descending, multi-field sort
          └── AuditingTest.java         5 tests — @CreatedDate populated, @LastModifiedDate
                                                  updated, updatable=false, @TestConfiguration
```

Total: **39 tests**, all passing.

---

## Key Takeaways

```
  JpaRepository         — extends CrudRepository + PagingAndSortingRepository
  Derived query methods — method name parsed to JPQL at startup; zero boilerplate
  @Query JPQL           — full control; uses entity/field names (not table/column)
  @Query nativeQuery    — raw SQL; database-specific syntax
  @Modifying            — required for UPDATE/DELETE; clears persistence context
  Interface projection  — JDK proxy; only declared columns fetched
  DTO projection        — JPQL 'new' expression; record; no proxy
  Page / Pageable       — LIMIT+OFFSET SQL + total COUNT; hasNext/hasPrevious
  Sort                  — ORDER BY; single or multi-field; ASC/DESC
  @CreatedDate          — set once on INSERT; updatable = false
  @LastModifiedDate     — updated on every flush
  @DataJpaTest          — JPA slice; transactional rollback; import auditing config
  @TestConfiguration    — supplements slice context; does NOT replace auto-config
```
