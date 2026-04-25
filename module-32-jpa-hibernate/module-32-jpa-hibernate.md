---
title: "Module 32 — JPA & Hibernate"
parent: "Phase 4 — Databases & Persistence"
nav_order: 32
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-32-jpa-hibernate/src){: .btn .btn-outline }

# Module 32 — JPA & Hibernate

JPA (Jakarta Persistence API) is an abstraction over JDBC.  Instead of writing
SQL manually, you describe your object model with annotations and let the ORM
generate SQL, manage connections, and track changes automatically.
Hibernate is the most widely used JPA implementation.

---

## Architecture

```
  ┌──────────────────────────────────────────────────────────────────────┐
  │  Your Application                                                    │
  │  Author a = em.find(Author.class, 1L);                               │
  │  a.setName("New Name");  // no SQL yet                               │
  │  tx.commit();            // Hibernate flushes: generates UPDATE       │
  └──────────────────────────────┬───────────────────────────────────────┘
                                 │
                    JPA API  (jakarta.persistence.*)
                                 │
  ┌──────────────────────────────▼───────────────────────────────────────┐
  │  Hibernate ORM                                                       │
  │  ├── EntityManagerFactory — per application, expensive to build      │
  │  ├── EntityManager        — per request/transaction, cheap to create │
  │  ├── Persistence Context  — first-level cache; tracks managed objects│
  │  └── Session/SessionFactory — Hibernate's own API (superset of JPA) │
  └──────────────────────────────┬───────────────────────────────────────┘
                                 │  generates SQL
  ┌──────────────────────────────▼───────────────────────────────────────┐
  │  JDBC (Connection, PreparedStatement, ResultSet)                     │
  └──────────────────────────────┬───────────────────────────────────────┘
                                 │
  ┌──────────────────────────────▼───────────────────────────────────────┐
  │  Database  (H2, PostgreSQL, MySQL, …)                                │
  └──────────────────────────────────────────────────────────────────────┘
```

---

## Entity Lifecycle

```
                      new Author(...)
                            │
                         NEW / TRANSIENT
                      (no persistence context)
                            │
                     em.persist(author)
                            │
                         MANAGED
                      (persistence context tracks it)
                            │          │
              tx.commit()   │          │ em.detach(author)
            / em.close()    │          │ em.clear()
                            ▼          ▼
                        REMOVED    DETACHED
                   (DELETE on flush)   (changes NOT auto-saved)
                                          │
                                    em.merge(author)
                                          │
                                       MANAGED (fresh copy)
```

### Dirty Checking

```
  When em.find(Author.class, id) loads an entity, Hibernate stores a SNAPSHOT
  of its field values.  At flush time (before commit or explicit em.flush()),
  Hibernate compares current values against the snapshot:

  Author a = em.find(Author.class, id);  // snapshot: name="Alice"
  a.setName("Bob");                      // current: name="Bob"
  tx.commit();                           // diff found → generates UPDATE

  No em.persist() or em.merge() needed.  This is dirty checking.
```

---

## First-Level (L1) Cache — Persistence Context

```
  Within one EntityManager, every entity is identified by (type, id).
  Two calls to em.find() with the same id return the identical Java object:

  Author a1 = em.find(Author.class, 1L);   // DB hit, stored in L1 cache
  Author a2 = em.find(Author.class, 1L);   // L1 hit — same object reference
  assert a1 == a2;                          // ✓ same Java reference

  Benefits:
  ✓  Avoids redundant DB queries within a request
  ✓  Ensures consistent object identity
  ✓  Foundation for dirty checking (snapshot stored once per entity)

  Scope: EntityManager lifetime.  em.close() or em.clear() evicts the cache.
```

---

## Relationships

```
  ┌────────────────────┬────────────────────┬─────────────────────────────┐
  │  Relationship      │  JPA Annotation    │  Default FetchType          │
  ├────────────────────┼────────────────────┼─────────────────────────────┤
  │  Many books per    │  @ManyToOne        │  EAGER (often override LAZY)│
  │  one author        │  @OneToMany        │  LAZY                       │
  ├────────────────────┼────────────────────┼─────────────────────────────┤
  │  Book ↔ Detail     │  @OneToOne         │  EAGER                      │
  ├────────────────────┼────────────────────┼─────────────────────────────┤
  │  Books ↔ Tags      │  @ManyToMany       │  LAZY                       │
  └────────────────────┴────────────────────┴─────────────────────────────┘
```

### Bidirectional relationship rule

Always keep both sides consistent.  Use helper methods:

```java
// Author side:
public void addBook(Book book) {
    books.add(book);        // update inverse side
    book.setAuthor(this);   // update owning side (the FK column)
}
```

Forgetting to update the owning side (`book.setAuthor`) means the FK column
is never written — the relationship won't be persisted.

### Cascade and OrphanRemoval

```
  CascadeType.ALL        — any operation on parent propagates to children
  CascadeType.PERSIST    — only persist cascades
  CascadeType.REMOVE     — DELETE on parent cascades to children

  orphanRemoval = true   — child removed from parent's collection → DELETE
                           (more fine-grained than CascadeType.REMOVE)
```

---

## Fetch Types — LAZY vs EAGER

```
  EAGER — association loaded in the same SELECT (or an immediate second SELECT)
          when the owning entity is loaded.

      Book b = em.find(Book.class, id);
      b.getDetail(); // no extra SQL — detail was already fetched

  LAZY  — association loaded only when the field is first accessed.
          Accessing a LAZY field after the EntityManager is closed →
          LazyInitializationException.

      Book b = em.find(Book.class, id);
      b.getAuthor(); // triggers SELECT authors WHERE id = ?
```

---

## N+1 Problem and Fix

The most common JPA performance trap.  Loading N parents with a LAZY
collection triggers 1 + N queries — one for the parents and one per parent
to load each child collection.

```
  SELECT * FROM authors;                    ← 1 query

  SELECT * FROM books WHERE author_id = 1; ← for Author-1
  SELECT * FROM books WHERE author_id = 2; ← for Author-2
  SELECT * FROM books WHERE author_id = 3; ← for Author-3
                                              3 more queries — N+1
```

**Fix: JOIN FETCH**

```java
// BAD — N+1
List<Author> authors = em
    .createQuery("SELECT a FROM Author a", Author.class)
    .getResultList();
// Then accessing a.getBooks() for each author fires N extra SELECTs.

// GOOD — 1 query
List<Author> authors = em
    .createQuery("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books",
                 Author.class)
    .getResultList();
// Books are included in the JOIN — 0 extra queries.
```

Measure the difference with `SessionFactory.getStatistics()`:

```java
Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
stats.setStatisticsEnabled(true);
stats.clear();
// ... run queries ...
System.out.println(stats.getPrepareStatementCount()); // 4 without fix, 1 with
```

---

## JPQL vs SQL vs Criteria API

```
  SQL    — works on tables and columns:
           SELECT * FROM authors WHERE name = 'Jane'

  JPQL   — works on entity class names and field names (not table/column):
           SELECT a FROM Author a WHERE a.name = :name
           → portable, database-independent

  Criteria API — programmatic, type-safe (no strings):
           CriteriaBuilder cb = em.getCriteriaBuilder();
           CriteriaQuery<Author> cq = cb.createQuery(Author.class);
           Root<Author> root = cq.from(Author.class);
           cq.select(root).where(cb.equal(root.get("name"), "Jane"));
           → useful when query structure is built dynamically at runtime
```

### JPQL examples

```java
// Named parameter
em.createQuery("SELECT a FROM Author a WHERE a.email = :email", Author.class)
  .setParameter("email", "dickens@classic.com")
  .getSingleResult();

// JOIN (object traversal, not JOIN ON)
em.createQuery("SELECT b FROM Book b JOIN b.author a WHERE a.name = :name", Book.class)
  .setParameter("name", "Charles Dickens")
  .getResultList();

// Aggregate
em.createQuery("SELECT a.name, COUNT(b) FROM Author a LEFT JOIN a.books b GROUP BY a.name",
               Object[].class)
  .getResultList();

// JOIN FETCH (N+1 fix)
em.createQuery("SELECT DISTINCT a FROM Author a LEFT JOIN FETCH a.books", Author.class)
  .getResultList();
```

---

## Bean Validation 3.0 (Jakarta)

Constraint annotations live on entity fields and are enforced:
- Manually via `Validator.validate(entity)`
- Automatically by Hibernate before any flush (if a validator is present)
- By Spring's `@Valid` / `@Validated` in REST layers

```java
@NotBlank          — string must not be null, empty, or whitespace-only
@Email             — string must be a syntactically valid e-mail address
@DecimalMin("0.01")— BigDecimal value must be ≥ 0.01
@Min(1)            — integer value must be ≥ 1
@NotNull           — value must not be null
@Size(min=1,max=100) — string/collection length within bounds
@Pattern(regexp=…) — string must match regex
```

```java
Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
Set<ConstraintViolation<Author>> violations = validator.validate(author);
// violations.isEmpty() == true for valid entities
```

---

## Second-Level (L2) Cache

L1 cache (persistence context) is per EntityManager.  L2 cache is
per SessionFactory — shared across all EntityManagers and all requests.

```
  ┌──────────────────────────────────────────────────────┐
  │  SessionFactory / EntityManagerFactory               │
  │  ┌────────────────────────────────────────────────┐  │
  │  │  L2 Cache (shared, optional)                   │  │
  │  │  e.g. Caffeine / EHCache via hibernate-jcache  │  │
  │  │                                                │  │
  │  │  Author{id=1} ← served to ALL EntityManagers   │  │
  │  └────────────────────────────────────────────────┘  │
  │                                                      │
  │  EntityManager-1   EntityManager-2   …              │
  │  (L1 cache)        (L1 cache)                        │
  └──────────────────────────────────────────────────────┘
```

Enable per-entity:
```java
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Author { … }
```

Configure in `persistence.xml`:
```xml
<property name="hibernate.cache.use_second_level_cache"   value="true"/>
<property name="hibernate.cache.region.factory_class"
          value="org.hibernate.cache.jcache.JCacheRegionFactory"/>
```

L2 cache works best for reference data that changes rarely (countries, categories,
configuration).  Never cache entities that are frequently written.

---

## Transaction Management Pattern

```java
// Plain JPA (no Spring) — manual transaction management
boolean prev = conn.getAutoCommit();  // JpaUtil preserves caller's autoCommit
EntityTransaction tx = em.getTransaction();
tx.begin();
try {
    // ... entity operations ...
    tx.commit();
} catch (RuntimeException e) {
    if (tx.isActive()) tx.rollback();
    throw e;
}

// With Spring — declarative, no boilerplate
@Transactional
public void placeOrder(Order order) {
    // Spring's AOP proxy calls tx.begin() before and tx.commit()/rollback() after
    repository.save(order);
}
```

---

## Module 32 — What Was Built

```
  module-32-jpa-hibernate/
  ├── pom.xml           (Hibernate 6.6.2, H2, Hibernate Validator 8.0, JUnit 5.10.2)
  ├── src/main/resources/META-INF/persistence.xml
  └── src/
      ├── main/java/com/javatraining/jpa/
      │   ├── entity/
      │   │   ├── Author.java      — @OneToMany(cascade=ALL, orphanRemoval=true)
      │   │   ├── Book.java        — @ManyToOne(LAZY), @OneToOne(EAGER), @ManyToMany
      │   │   ├── Tag.java         — @ManyToMany inverse side
      │   │   └── BookDetail.java  — @OneToOne target, Bean Validation constraints
      │   └── config/
      │       └── JpaUtil.java     — createEmf(dbName), inTransaction() helpers
      └── test/java/com/javatraining/jpa/
          ├── EntityLifecycleTest.java   9 tests — NEW/MANAGED/DETACHED/REMOVED, dirty check, L1 cache
          ├── RelationshipsTest.java     7 tests — OneToMany, OneToOne, ManyToMany, cascade, orphan
          ├── FetchAndNPlusOneTest.java  4 tests — LAZY/EAGER, N+1 with statistics, JOIN FETCH
          ├── QueriesTest.java           8 tests — JPQL select/like/join/group-by, Criteria API
          └── ValidationTest.java        6 tests — @NotBlank, @Email, @DecimalMin, @Min
```

Total: **34 tests**, all passing.

---

## Key Takeaways

```
  EntityManagerFactory  — create once at startup (expensive)
  EntityManager         — create per request/transaction (cheap)
  Dirty checking        — modify MANAGED entities; no explicit save needed
  L1 cache              — same EntityManager, same id → same Java object
  LAZY vs EAGER         — LAZY = load on demand; EAGER = load immediately
  N+1                   — diagnose with Statistics; fix with JOIN FETCH
  Cascade               — propagate lifecycle operations through the graph
  orphanRemoval=true    — remove from collection → DELETE from DB
  JPQL                  — entity/field names, not table/column names
  Criteria API          — programmatic queries (useful for dynamic conditions)
  Bean Validation       — constraints on entity fields; validated before flush
```
{% endraw %}
