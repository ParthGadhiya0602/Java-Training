---
title: "10 — OOP: Encapsulation"
parent: "Phase 1 — Fundamentals"
nav_order: 10
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-10-oop-encapsulation/src){: .btn .btn-outline }

# Module 10 — OOP: Encapsulation

## What You Will Learn

| Topic | Key Insight |
|---|---|
| Access modifiers | `private` > package-private > `protected` > `public` — use the most restrictive that works |
| Invariant enforcement | Validate in setters/constructors; reject bad state at the boundary, not after |
| Immutability | No setters + `final` fields + defensive copies = thread-safe without synchronisation |
| Defensive copy (in) | Copy mutable arguments in the constructor before storing them |
| Defensive copy (out) | Copy mutable fields before returning them from getters |
| Mutable vs Immutable | When each is appropriate; why you should default to immutable |
| Builder pattern | Solves the telescoping-constructor problem; readable, safe, optional fields |
| Step Builder | Forces callers to set required fields in order — compile-time validation |
| Copy builder | Create a modified copy of an immutable object via `toBuilder()` |

---

## Access Modifier Visibility

```
Modifier         Same class   Same package   Subclass   Everywhere
─────────────────────────────────────────────────────────────────
private             ✓              ✗             ✗          ✗
(package-private)   ✓              ✓             ✗          ✗
protected           ✓              ✓             ✓          ✗
public              ✓              ✓             ✓          ✓
```

---

## Defensive Copy Pattern

```
// UNSAFE — stores the external array reference directly
class Schedule {
    private final Date[] slots;
    Schedule(Date[] slots) { this.slots = slots; }     // ← aliasing!
}
Date[] arr = { new Date() };
Schedule s = new Schedule(arr);
arr[0] = null;   // MUTATES the schedule's internal state!

// SAFE — defensive copy on the way IN
class Schedule {
    private final Date[] slots;
    Schedule(Date[] slots) { this.slots = slots.clone(); }  // ← copy
}

// SAFE — defensive copy on the way OUT
Date[] getSlots() { return slots.clone(); }   // caller can't affect internals
```

---

## Immutability Checklist

```
□  All fields are private final
□  No setters
□  Class is final (or effectively sealed)  — prevents subclass mutation
□  Mutable fields get defensive copies IN  (constructor)
□  Mutable fields get defensive copies OUT (getters)
□  No methods leak internal mutable references (e.g. via Collections.unmodifiableList)
```

---

## Builder Pattern (Telescoping Constructor Problem)

```
// Telescoping constructors — unreadable and error-prone
new User("Alice", "alice@x.com", null, null, false, true, "UTC");

// Builder — readable, order-independent optional fields
User user = User.builder()
    .name("Alice")
    .email("alice@x.com")
    .timezone("UTC")
    .verified(true)
    .build();
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `AccessModifiers.java` | `private`/`protected`/package-private/`public`; encapsulated invariants |
| `ImmutableTypes.java` | Immutable class checklist; defensive copy in/out; `withX` copy pattern |
| `BuilderPattern.java` | Classic Builder, Step Builder (compile-time required fields), copy builder |
| `UserProfile.java` | Full design: immutable core + builder + validation + defensive copies |

---

## Running

```bash
cd module-10-oop-encapsulation
mvn test
```
