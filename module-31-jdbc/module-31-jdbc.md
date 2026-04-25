---
title: "Module 31 - JDBC"
parent: "Phase 4 - Databases & Persistence"
nav_order: 31
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-31-jdbc/src){: .btn .btn-outline }

# Module 31 - JDBC

JDBC is the lowest-level bridge between Java and a relational database.
Every higher abstraction - JPA, Spring Data, MyBatis - ultimately generates SQL
and sends it through JDBC. Understanding JDBC directly means you can read what
your ORM is doing, diagnose connection-pool exhaustion, and write fast bulk
operations that no ORM can match.

---

## JDBC Architecture

```
  ┌───────────────────────────────────────────────────────────────────────┐
  │  Your Application                                                     │
  │                                                                       │
  │  DriverManager.getConnection(url, user, pass)                         │
  │  conn.prepareStatement(sql)           // create a parameterised stmt  │
  │  ps.setString(1, value)               // bind a parameter             │
  │  ps.executeQuery()  /  ps.executeUpdate()  // execute                 │
  │  rs.next() / rs.getString("col")      // navigate ResultSet           │
  └──────────────────────────────┬────────────────────────────────────────┘
                                 │
                         java.sql  (JDBC API - stable interface)
                                 │
  ┌──────────────────────────────▼────────────────────────────────────────┐
  │  JDBC Driver  (e.g. org.h2.Driver, org.postgresql.Driver)            │
  │  Translates JDBC calls into the database's wire protocol.            │
  │  Registered automatically via java.sql.Driver service-loader         │
  │  (META-INF/services) - no Class.forName() needed since JDBC 4.0.     │
  └──────────────────────────────┬────────────────────────────────────────┘
                                 │  TCP / unix socket / in-memory
  ┌──────────────────────────────▼────────────────────────────────────────┐
  │  Database  (H2, PostgreSQL, MySQL, Oracle, …)                         │
  └───────────────────────────────────────────────────────────────────────┘
```

---

## Core API Classes

```
  java.sql.DriverManager     - opens raw physical connections
  java.sql.Connection        - a session with the database; owns transactions
  java.sql.Statement         - ad-hoc SQL (never use with user input - SQL injection risk)
  java.sql.PreparedStatement - parameterised SQL; safe, faster for repeated execution
  java.sql.CallableStatement - stored procedure calls
  java.sql.ResultSet         - cursor over query results; one row at a time
  java.sql.Savepoint         - named checkpoint within a transaction
  java.sql.SQLException      - checked exception for all JDBC failures
```

---

## Statement vs PreparedStatement

```
  UNSAFE - Statement with string concatenation:
  ┌───────────────────────────────────────────────────────────────────────┐
  │  String input = "'; DROP TABLE products; --";                         │
  │  stmt.execute("SELECT * FROM products WHERE name = '" + input + "'");  │
  │                                                                       │
  │  Sent to DB:  SELECT * FROM products WHERE name = '';                 │
  │               DROP TABLE products; --'                                │
  │                                                                       │
  │  Result: table destroyed.  This is SQL injection.                     │
  └───────────────────────────────────────────────────────────────────────┘

  SAFE - PreparedStatement with parameter binding:
  ┌───────────────────────────────────────────────────────────────────────┐
  │  PreparedStatement ps = conn.prepareStatement(                        │
  │      "SELECT * FROM products WHERE name = ?");                        │
  │  ps.setString(1, input);   // entire input treated as a data value    │
  │                                                                       │
  │  Sent to DB:  SELECT * FROM products WHERE name = ?                   │
  │  Bound:                                           ↑                   │
  │               "'; DROP TABLE products; --"  ← stored as a literal     │
  │                                                                       │
  │  Result: zero rows (no product with that name).  Table unharmed.      │
  └───────────────────────────────────────────────────────────────────────┘

  Benefits of PreparedStatement beyond security:
    ✓  Query plan cached by the DB after first execution - faster on repeat
    ✓  Correct type handling - setInt()/setBigDecimal() avoid quoting bugs
    ✓  Null safety - setNull() instead of injecting the literal "NULL"
```

---

## ResultSet Navigation

```
  ResultSet starts BEFORE the first row:

  rs.next() ─► row 1 ─► row 2 ─► row 3 ─► (returns false - no more rows)

  Common access methods:
    rs.getInt("id")           // by column name - resilient to column reordering
    rs.getString("name")
    rs.getBigDecimal("price")
    rs.getTimestamp("created_at")

  Null check:
    rs.getInt("qty");           // returns 0 for SQL NULL - use wasNull()
    rs.wasNull()                // true if the last column read was NULL

  Always close ResultSet (try-with-resources handles this automatically).
```

---

## Generated Keys

```java
// Statement.RETURN_GENERATED_KEYS tells the driver to capture auto-increment values
PreparedStatement ps = conn.prepareStatement(
    "INSERT INTO products (name, price, stock_qty) VALUES (?, ?, ?)",
    Statement.RETURN_GENERATED_KEYS);

ps.setString(1, "Widget");
ps.setBigDecimal(2, new BigDecimal("9.99"));
ps.setInt(3, 100);
ps.executeUpdate();

try (ResultSet keys = ps.getGeneratedKeys()) {
    if (keys.next()) {
        int id = keys.getInt(1);   // the auto-generated primary key
    }
}
```

---

## Transaction Management

Every JDBC operation is in a transaction. By default, `autoCommit = true`
means each statement commits immediately. For multi-statement atomicity,
turn it off explicitly.

```
  conn.setAutoCommit(false);          ← BEGIN transaction

  ├── UPDATE products SET stock_qty = stock_qty - 5 WHERE id = ?
  ├── INSERT INTO orders (product_id, qty, …) VALUES (?, ?, …)
  │
  ├── ALL SUCCEEDED  →  conn.commit()     ← changes become permanent
  │
  └── ANY FAILURE    →  conn.rollback()   ← all changes reverted
         partial      →  conn.rollback(savepoint)  ← reverts only from SP

  conn.setAutoCommit(true);           ← restore default (critical for pools)
```

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │  Pattern: always save/restore autoCommit - safe with pooled conns   │
  │                                                                     │
  │  boolean prev = conn.getAutoCommit();                               │
  │  conn.setAutoCommit(false);                                         │
  │  try {                                                              │
  │      // ... SQL statements ...                                      │
  │      conn.commit();                                                 │
  │  } catch (SQLException e) {                                         │
  │      conn.rollback();                                               │
  │      throw e;                                                       │
  │  } finally {                                                        │
  │      conn.setAutoCommit(prev);   ← next pool borrower is clean      │
  │  }                                                                  │
  └─────────────────────────────────────────────────────────────────────┘
```

### Savepoints - Partial Rollback

```
  Savepoint sp = conn.setSavepoint("afterStep1");

  BEGIN
    ├── statement 1   ← committed by rollback(sp) if something fails later
    ├── [savepoint]
    ├── statement 2   ← reverted by rollback(sp)
    └── rollback(sp)  → statement 2 undone; statement 1 still pending

  conn.commit() → finalises statement 1 only.
```

---

## HikariCP Connection Pool

Opening a raw TCP connection to a database costs 5–50 ms: DNS lookup,
TCP handshake, TLS, authentication. Under 100 req/s that alone is 0.5–5 s
of wasted latency. A pool amortises that cost by keeping connections alive
and lending them out.

```
  ┌────────────────────────────────────────────────────────────────────┐
  │  HikariCP Pool (10 max connections, 2 minimum idle)                │
  │                                                                    │
  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐  (idle)        │
  │  │ con │ │ con │ │ con │ │ con │ │ con │ │ con │                  │
  │  └─────┘ └─────┘ └──┬──┘ └─────┘ └─────┘ └─────┘                │
  │                      │  getConnection() - borrow                  │
  │                      ▼                                            │
  │                Application  ←  uses connection (μs overhead)      │
  │                      │  connection.close() - RETURNS to pool       │
  │                      ▼                                            │
  │                 ┌──────┐  (idle again - NOT physically closed)    │
  │                 │ con  │                                           │
  │                 └──────┘                                           │
  └────────────────────────────────────────────────────────────────────┘

  Key settings:
    maximumPoolSize   = 10   never open more than this many physical conns
    minimumIdle       = 2    keep 2 warm even during quiet periods
    connectionTimeout = 3 s  throw if no connection available within 3 s
    idleTimeout       = 10 m close connections idle for 10 minutes
    maxLifetime       = 30 m replace connection after 30 minutes (avoids server-side drops)
```

**Rule:** `connection.close()` returns the connection to the pool - it does NOT
close the physical socket. The pool recycles it for the next caller.

---

## Batch Processing

When inserting or updating many rows, individual `executeUpdate()` calls make
one network round-trip per statement. `executeBatch()` sends all statements in
a single round trip.

```
  Without batch - N round trips:
  ┌───────┐   SQL₁   ┌────┐          ┌───────┐   SQL₂   ┌────┐
  │  App  │ ────────► │ DB │    …     │  App  │ ────────► │ DB │   × N
  └───────┘ ◄──────── └────┘          └───────┘ ◄──────── └────┘
  Total: N × (network latency + DB parse + DB execute)

  With executeBatch() - 1 round trip:
  ┌───────┐  SQL₁…SQLₙ  ┌────┐
  │  App  │ ────────────► │ DB │  × 1   (1 network + N executes)
  └───────┘ ◄──────────── └────┘
```

```java
PreparedStatement ps = conn.prepareStatement(
    "INSERT INTO products (name, price, stock_qty) VALUES (?, ?, ?)");

for (Product p : products) {
    ps.setString(1, p.name());
    ps.setBigDecimal(2, p.price());
    ps.setInt(3, p.stockQty());
    ps.addBatch();       // ← queues locally, no network call yet
}

int[] counts = ps.executeBatch();   // ← ONE network call for all rows
// counts[i] = rows affected by the i-th batched statement (usually 1)
```

---

## BigDecimal for Money

Never store or calculate money with `double` - binary floating-point cannot
represent most decimal fractions exactly:

```java
// WRONG
double price = 0.10 + 0.20;
System.out.println(price);  // 0.30000000000000004 ← not 0.30

// RIGHT - BigDecimal uses arbitrary-precision decimal arithmetic
BigDecimal a = new BigDecimal("0.10");
BigDecimal b = new BigDecimal("0.20");
System.out.println(a.add(b));  // 0.30

// Use BigDecimal.valueOf(double) when accepting a double - it uses
// Double.toString() which gives the shortest exact representation:
BigDecimal price = BigDecimal.valueOf(9.99);  // "9.99" exactly
```

DECIMAL(10,2) in SQL maps to `BigDecimal` in JDBC:

```java
ps.setBigDecimal(1, product.price());      // INSERT / UPDATE
BigDecimal price = rs.getBigDecimal("price");  // SELECT
```

---

## Module 31 - What Was Built

```
  module-31-jdbc/
  ├── pom.xml               (H2 2.3.232, HikariCP 5.1.0, JUnit 5.10.2)
  └── src/
      ├── main/java/com/javatraining/jdbc/
      │   ├── model/
      │   │   ├── Product.java          ← record; BigDecimal price; Product.of() factory
      │   │   └── Order.java            ← record; references product via id
      │   ├── core/
      │   │   ├── ConnectionFactory.java     ← DriverManager wrapper; no pooling
      │   │   ├── HikariConnectionPool.java  ← pool wrapper; active/total metrics
      │   │   └── DatabaseInitializer.java   ← idempotent CREATE/DROP TABLE
      │   ├── repository/
      │   │   ├── ProductRepository.java     ← CRUD via PreparedStatement
      │   │   └── OrderRepository.java       ← placeOrder() with full transaction
      │   └── batch/
      │       └── BatchImporter.java         ← insertBatch / updatePricesBatch
      └── test/java/com/javatraining/jdbc/
          ├── JdbcCoreTest.java        15 tests - CRUD, SQL injection, BigDecimal
          ├── TransactionTest.java      9 tests - commit, rollback, savepoints, autoCommit restore
          ├── ConnectionPoolTest.java   9 tests - pool metrics, multi-borrow, data persistence
          └── BatchTest.java            7 tests - batch insert/update, large volume, empty batch
```

Total: **40 tests**, all passing.

---

## Key Takeaways

```
  PreparedStatement        - always; never Statement with user input
  try-with-resources       - always; prevents connection/statement/resultset leaks
  BigDecimal               - always for money; never double
  Connection.close()       - returns to pool; does not close socket
  setAutoCommit(false)     - begin explicit transaction
  rollback() in catch      - undo partial changes on failure
  restore autoCommit       - in finally; pool safety
  addBatch/executeBatch    - 10–100× faster for bulk DML
```

{% endraw %}
