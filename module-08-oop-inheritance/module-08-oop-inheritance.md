---
title: "08 — OOP: Inheritance"
parent: "Phase 1 — Fundamentals"
nav_order: 8
render_with_liquid: false
---

# Module 08 — OOP: Inheritance & Polymorphism

## What You Will Learn

| Topic | Key Insight |
|---|---|
| `extends` & constructor chaining | `super(...)` must be first in child constructor; JVM walks the chain upward |
| Method overriding | `@Override` is compile-time proof; return type may be covariant |
| `super` method calls | Call the parent version explicitly when you want to augment, not replace |
| `final` | `final class` → no subclassing; `final method` → no overriding; `final field` → assign once |
| Abstract classes | Template Method pattern — skeleton in parent, steps deferred to children |
| Polymorphism | Reference type determines which methods you can *call*; object type determines which runs |
| Casting | Widening (safe, implicit); narrowing (risky, needs explicit cast + `instanceof` check) |
| `instanceof` pattern matching | Java 16+ — eliminates the cast-after-check boilerplate |
| Sealed classes (Java 17+) | Closed hierarchy — compiler knows every subtype; exhaustive `switch` without `default` |
| Liskov Substitution Principle | Every subtype must be substitutable for its parent without breaking correctness |

---

## Inheritance Memory Model

```
                     Object
                       │
              ┌────────┴────────┐
           Animal            (other)
         fields: name, age
         method: eat(), sleep()
              │
       ┌──────┴──────┐
      Dog           Cat
  field: breed    field: indoor
  override: speak()  override: speak()
  add: fetch()

Stack                   Heap
─────                   ────────────────────────────
Animal a ───────────────►  Dog object
                           ├─ Animal part: name="Rex", age=3
                           └─ Dog part:    breed="Labrador"

// a.speak() → Dog.speak()  (runtime dispatch on actual type)
// a.fetch() → compile error (reference type is Animal)
```

---

## Method Dispatch (Virtual Method Table)

```
  compile time                  runtime
  ─────────────                 ─────────────────────────
  Animal a = new Dog(...)
  a.speak()  ───────────────►   Dog.speak()   ← actual type wins
  a.eat()    ───────────────►   Animal.eat()  ← not overridden, falls back
```

---

## Casting Rules

```
Widening  (always safe):   Dog d = new Dog();
                           Animal a = d;          // implicit

Narrowing (risky):         Animal a = new Dog();
                           Dog d = (Dog) a;        // explicit, ClassCastException if wrong

Pattern match (Java 16+):  if (a instanceof Dog dog) {
                               dog.fetch();        // no cast needed — dog is Dog
                           }

Switch pattern (Java 21):  switch (shape) {
                               case Circle c    -> ...
                               case Rectangle r -> ...
                           }
```

---

## Sealed Class Hierarchy

```
sealed interface Shape permits Circle, Rectangle, Triangle {}

// Compiler knows the COMPLETE set of subtypes.
// switch on Shape can be exhaustive without default:

String area = switch (shape) {
    case Circle    c -> "π·r²";
    case Rectangle r -> "w·h";
    case Triangle  t -> "½·b·h";
    // no default needed — sealed guarantees exhaustiveness
};
```

---

## Liskov Substitution Principle

```
Rule: if S extends P, then anywhere P is expected, S must work correctly.

VIOLATION example:
  class Rectangle { setWidth(w); setHeight(h); area() = w*h; }
  class Square extends Rectangle {
      setWidth(w)  { super.setWidth(w);  super.setHeight(w); } // ← breaks LSP
      setHeight(h) { super.setWidth(h);  super.setHeight(h); }
  }
  // Code that expects Rectangle:
  Rectangle r = new Square();
  r.setWidth(5); r.setHeight(3);
  assert r.area() == 15;  // FAILS — Square made it 9
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `InheritanceBasics.java` | `extends`, `super`, constructor chaining, `@Override`, `final`, covariant return |
| `PolymorphismDemo.java` | Dynamic dispatch, casting, `instanceof` pattern matching, LSP |
| `SealedHierarchy.java` | Sealed classes/interfaces, exhaustive switch, pattern matching |
| `ShapeCalculator.java` | Full design: abstract class (Template Method), sealed subtypes, polymorphic processing |

---

## Running

```bash
cd module-08-oop-inheritance
mvn test
mvn compile exec:java -Dexec.mainClass=com.javatraining.inheritance.ShapeCalculator
```
