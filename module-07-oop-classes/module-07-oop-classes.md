---
title: "07 — OOP: Classes & Objects"
parent: "Phase 1 — Fundamentals"
nav_order: 7
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-07-oop-classes/src){: .btn .btn-outline }

# Module 07 — OOP: Classes & Objects

## What You Will Learn

| Topic | Key Insight |
|---|---|
| Class anatomy | fields, constructors, methods, static members — where each lives in memory |
| Constructor chaining | `this(...)` eliminates duplication; telescoping constructors vs builder |
| `this` reference | disambiguation, chaining, passing self as argument |
| Static members | class-level state and behaviour; initialisation order |
| Object lifecycle | allocation on heap, GC eligibility, `finalize` is dead — use `Cleaner` |
| `equals` / `hashCode` contract | symmetry, transitivity, consistency; why violating it breaks `HashMap` |
| `toString` | always override it — the debugger and logs will thank you |
| Records (Java 16+) | immutable data carriers; compact constructor for validation |
| `var` with objects | type inference at declaration site |

---

## Memory Layout

```
Stack                       Heap
─────                       ────────────────────────────────────────
main() frame                ┌─────────────────────┐
  p ──────────────────────► │  Person object       │
                            │  name: ──────────────┼──► "Alice" (String pool)
                            │  age:  30            │
                            │  email: ─────────────┼──► "alice@x.com"
                            └─────────────────────┘

Two variables, one object:
  a ──────────────────────► ┌───────────────┐
  b ───────────────────────►│  same object  │
                            └───────────────┘
  a == b   → true  (same reference)
  a.equals(b) → true (same content, if properly implemented)
```

---

## equals / hashCode Contract

```
                 ┌──────────────────────────────────────────┐
                 │            THE CONTRACT                   │
                 │                                          │
  Reflexive  ──► │  x.equals(x) == true                    │
  Symmetric  ──► │  x.equals(y) == y.equals(x)             │
  Transitive ──► │  x.equals(y) && y.equals(z) → x.equals(z)│
  Consistent ──► │  same result across multiple calls        │
  Null-safe  ──► │  x.equals(null) == false                 │
                 │                                          │
                 │  CRITICAL LINK:                          │
                 │  x.equals(y) → x.hashCode()==y.hashCode()│
                 │  (converse NOT required)                 │
                 └──────────────────────────────────────────┘

Violation consequence:
  HashSet<Point> set = new HashSet<>();
  set.add(new Point(1, 2));
  set.contains(new Point(1, 2));  // FALSE if hashCode broken!
```

---

## Static Initialisation Order

```
Class loaded
    │
    ▼
1. static fields (in declaration order)
2. static initialiser blocks (in declaration order)
    │
    ▼
new MyClass()
    │
    ▼
3. instance fields (in declaration order)
4. instance initialiser blocks
5. constructor body
```

---

## Record vs Class

```
// class — 30+ lines boilerplate
class Point {
    private final int x, y;
    public Point(int x, int y) { this.x = x; this.y = y; }
    public int x() { return x; }
    public int y() { return y; }
    @Override public boolean equals(Object o) { ... }
    @Override public int hashCode() { ... }
    @Override public String toString() { ... }
}

// record — 1 line, same semantics
record Point(int x, int y) {}

// compact constructor — add validation
record Point(int x, int y) {
    Point {                              // no parameter list here
        if (x < 0 || y < 0)
            throw new IllegalArgumentException("negative coordinate");
        // x and y are implicitly assigned after the block
    }
}
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `ClassAnatomy.java` | Fields, constructors, `this`, static members, initialisation order |
| `EqualsHashCode.java` | Correct `equals`/`hashCode` with all five contract rules, `HashMap`/`HashSet` behaviour |
| `RecordsDemo.java` | Records, compact constructors, record patterns (Java 21), `with`-style copying |
| `BankAccount.java` | Full class design: encapsulation, invariants, `equals`/`hashCode`, `toString`, static factory, object lifecycle |

---

## Running

```bash
cd module-07-oop-classes
mvn test          # compile + run all JUnit 5 tests
mvn compile exec:java -Dexec.mainClass=com.javatraining.oop.ClassAnatomy
```
