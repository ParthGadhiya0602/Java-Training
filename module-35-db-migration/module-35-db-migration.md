---
title: "Module 35 — Database Migration"
nav_order: 35
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-35-db-migration/src){: .btn .btn-outline }

# Module 35 — Database Migration

Two complementary migration tools:
**Flyway** — SQL-first, versioned scripts with a strict `V{n}__description.sql` naming convention;
**Liquibase** — database-agnostic changeSets in YAML/XML/JSON with built-in rollback blocks.

---

## Why Schema Migration Tools?

```
  Without a migration tool          With a migration tool
  ──────────────────────────────    ──────────────────────────────────────
  "Run this ALTER on prod"          Each change is a versioned file in Git
  Manual, error-prone               Applied automatically on startup
  No audit trail                    Full history in schema_history table
  Hard to reproduce in test         Test DB = prod DB at the same version
  Rollback = manual SQL             Liquibase: rollback block in changeSet
```

---

## Flyway

### Versioned Migration Naming

```
  db/migration/
  ├── V1__create_employees.sql      ← V{version}__{description}.sql
  ├── V2__create_departments.sql        two underscores separate version from desc
  ├── V3__seed_data.sql
  ├── V4__add_department_id_nullable.sql
  ├── V5__backfill_department_id.sql
  └── V6__make_department_id_not_null.sql

  Rules:
    Version must increase monotonically — gaps allowed (1, 2, 5, 10)
    Description is free text (underscores become spaces in history)
    Once applied, a script MUST NOT change (Flyway checksums it)
```

### How Flyway Runs

```
  Startup
    │
    ▼  Connect to DB
    │
    ▼  Read flyway_schema_history table
    │     (created automatically on first run)
    │
    ▼  Scan classpath:db/migration for V*.sql files
    │
    ▼  For each unapplied script (in version order):
    │     execute SQL
    │     record version + checksum in schema_history
    │
    ▼  Application starts
```

### Flyway Spring Boot Setup

```java
// pom.xml — just add the dependency, Spring Boot auto-configures the rest:
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

// application.properties:
spring.flyway.enabled=true            // default true when flyway-core is on classpath
// scripts go in: src/main/resources/db/migration/
```

### Migration Scripts

```sql
-- V1__create_employees.sql — initial table
CREATE TABLE employees (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    salary     DECIMAL(10,2) NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V2__create_departments.sql — new table (no existing data affected)
CREATE TABLE departments (
    id   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- V3__seed_data.sql — INSERT reference data
INSERT INTO departments (name) VALUES ('Engineering'), ('Marketing'), ('Finance');
INSERT INTO employees (name, email, salary, active) VALUES
    ('Alice', 'alice@example.com', 95000.00, TRUE), ...;
```

---

### Zero-Downtime Column Addition

```
  Problem: Add a NOT NULL FK column to a live table that already has rows.
  Naive approach:  ALTER TABLE employees ADD COLUMN department_id BIGINT NOT NULL;
                   → Fails: existing rows violate NOT NULL immediately.

  3-step zero-downtime pattern:
  ─────────────────────────────────────────────────────────────────────────────
  V4  ADD COLUMN ... (nullable)     Old and new app code both work. No lock.
  V5  UPDATE ... SET column = ...   Backfill existing rows batch by batch.
  V6  ALTER COLUMN ... NOT NULL     Safe: every row already has a value.
```

```sql
-- V4__add_department_id_nullable.sql
ALTER TABLE employees ADD COLUMN department_id BIGINT;
ALTER TABLE employees
    ADD CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id) REFERENCES departments (id);

-- V5__backfill_department_id.sql
UPDATE employees SET department_id = 1 WHERE department_id IS NULL;

-- V6__make_department_id_not_null.sql
ALTER TABLE employees ALTER COLUMN department_id BIGINT NOT NULL;
```

### Flyway Metadata

```java
@Autowired Flyway flyway;

// All applied migrations:
MigrationInfo[] applied = flyway.info().applied();

// Check migration count:
applied.length                            // 6

// Inspect each migration:
applied[0].getVersion().getVersion()      // "1"
applied[0].getDescription()              // "create employees"
applied[0].getState()                    // SUCCESS
applied[0].getChecksum()                 // CRC32 of the SQL file
```

---

## Liquibase

### Changelog Structure

```
  db/changelog/
  ├── db.changelog-master.yaml       ← root file: includes all others
  ├── 001-create-products.yaml
  ├── 002-seed-products.yaml
  └── 003-add-description.yaml

  Each changeSet has:
    id      — unique string identifier
    author  — who wrote it
    changes — list of operations (createTable, addColumn, insert, ...)
    rollback — how to undo this changeSet
```

### Master Changelog

```yaml
# db.changelog-master.yaml
databaseChangeLog:
  - include:
      file: db/changelog/001-create-products.yaml
  - include:
      file: db/changelog/002-seed-products.yaml
  - include:
      file: db/changelog/003-add-description.yaml
```

### ChangeSet Examples

```yaml
# 001-create-products.yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-products
      author: training
      changes:
        - createTable:
            tableName: products
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints: {primaryKey: true, nullable: false}
              - column:
                  name: price
                  type: DECIMAL(10,2)
                  constraints: {nullable: false}
              - column:
                  name: in_stock
                  type: BOOLEAN
                  defaultValueBoolean: true
                  constraints: {nullable: false}
      rollback:
        - dropTable:
            tableName: products

# 003-add-description.yaml
databaseChangeLog:
  - changeSet:
      id: 003-add-description
      author: training
      changes:
        - addColumn:
            tableName: products
            columns:
              - column:
                  name: description
                  type: VARCHAR(500)
                  constraints: {nullable: true}
      rollback:
        - dropColumn:
            tableName: products
            columnName: description
```

### Liquibase Spring Boot Setup

```java
// pom.xml:
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>

// application.properties:
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
```

### Liquibase Metadata

```
  Table: DATABASECHANGELOG
  ─────────────────────────────────────────────────────────────────────────────
  ID                       AUTHOR      DATEEXECUTED    DESCRIPTION
  001-create-products      training    2024-01-01      createTable tableName=products
  002-seed-products        training    2024-01-01      insert tableName=products (x3)
  003-add-description      training    2024-01-01      addColumn tableName=products

  Table: DATABASECHANGELOGLOCK — prevents concurrent migrations
```

---

## Flyway vs Liquibase

```
  ┌──────────────────────┬──────────────────────────┬────────────────────────────┐
  │                      │  Flyway                  │  Liquibase                 │
  ├──────────────────────┼──────────────────────────┼────────────────────────────┤
  │  Script format       │  Plain SQL               │  YAML / XML / JSON / SQL   │
  │  Rollback            │  No built-in rollback    │  rollback block per changeSet│
  │  Versioning          │  V{n}__ prefix           │  id + author per changeSet │
  │  Learning curve      │  Lower — it's just SQL   │  Higher — own DSL          │
  │  DB portability      │  Low (SQL is DB-specific)│  High (abstracted ops)     │
  │  History table       │  flyway_schema_history   │  DATABASECHANGELOG         │
  │  Best for            │  SQL-fluent teams        │  Multi-DB portability      │
  └──────────────────────┴──────────────────────────┴────────────────────────────┘
```

---

## Testing Two Tools on the Same DB

```
  Problem: FlywayMigrationTest and LiquibaseMigrationTest both start
  Spring, both load H2, and H2 is in-memory per URL.  If they share
  the same URL, Flyway's schema_history and Liquibase's DATABASECHANGELOG
  table collide — and whichever context starts second sees an already-migrated
  DB from the other tool.

  Fix: give each test class its own H2 URL.

  application.properties (default):
    spring.datasource.url=jdbc:h2:mem:flywaydb;DB_CLOSE_DELAY=-1
    spring.flyway.enabled=true
    spring.liquibase.enabled=false

  LiquibaseMigrationTest — @TestPropertySource overrides:
    spring.datasource.url=jdbc:h2:mem:liquibasedb;DB_CLOSE_DELAY=-1
    spring.flyway.enabled=false
    spring.liquibase.enabled=true
    spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

  Each test class gets a clean, isolated H2 instance — no interference.
```

```java
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.liquibase.enabled=true",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml",
        "spring.datasource.url=jdbc:h2:mem:liquibasedb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class LiquibaseMigrationTest { ... }
```

---

## Module 35 — What Was Built

```
  module-35-db-migration/
  ├── pom.xml     (Spring Boot 3.3.5, spring-boot-starter-jdbc,
  │               flyway-core, liquibase-core, H2 runtime, spring-boot-starter-test)
  └── src/
      ├── main/
      │   ├── java/com/javatraining/migration/
      │   │   └── DbMigrationApplication.java
      │   └── resources/
      │       ├── application.properties    (Flyway ON, Liquibase OFF)
      │       ├── db/migration/             (Flyway SQL scripts)
      │       │   ├── V1__create_employees.sql
      │       │   ├── V2__create_departments.sql
      │       │   ├── V3__seed_data.sql
      │       │   ├── V4__add_department_id_nullable.sql   ← zero-downtime step 1
      │       │   ├── V5__backfill_department_id.sql       ← zero-downtime step 2
      │       │   └── V6__make_department_id_not_null.sql  ← zero-downtime step 3
      │       └── db/changelog/             (Liquibase YAML changeSets)
      │           ├── db.changelog-master.yaml
      │           ├── 001-create-products.yaml
      │           ├── 002-seed-products.yaml
      │           └── 003-add-description.yaml
      └── test/
          ├── java/com/javatraining/migration/
          │   ├── FlywayMigrationTest.java    10 tests — schema structure, seed data,
          │   │                                          zero-downtime steps, migration history
          │   └── LiquibaseMigrationTest.java  6 tests — schema structure, seed data,
          │                                              changeSet history
          └── resources/
              └── logback-test.xml
```

Flyway tests: **10 passing**.
Liquibase tests: **6 passing**.
Total: **16 passing**.

---

## Key Takeaways

```
  Flyway naming     V{version}__{description}.sql — monotonically increasing version
  Checksums         Applied scripts are locked — Flyway fails if you modify one
  Zero-downtime     nullable → backfill → NOT NULL: three migrations, not one
  Flyway bean       flyway.info().applied() — full history with state + checksum

  Liquibase id      changeSet identified by id + author (not filename or version)
  rollback block    Explicit undo SQL/operation next to the change — not automatic
  YAML DSL          createTable / addColumn / insert / dropTable / dropColumn
  DATABASECHANGELOG — Liquibase's own history table (like flyway_schema_history)

  Isolation trick   Give each test class its own H2 URL (mem:flywaydb vs mem:liquibasedb)
                    to prevent Flyway and Liquibase from interfering with each other
```
{% endraw %}
