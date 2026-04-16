---
title: "09 — OOP: Interfaces"
parent: "Phase 1 — Fundamentals"
nav_order: 9
---

# Module 09 — OOP: Interfaces & Abstract Classes

## What You Will Learn

| Topic | Key Insight |
|---|---|
| Interface fundamentals | Contract only — no state (except constants); a class can implement many |
| `default` methods | Backwards-compatible behaviour added to an interface; resolved by proximity |
| `static` methods on interfaces | Utility factories tied to the type — `Comparator.comparing(...)` is the pattern |
| `private` methods on interfaces | Shared helper logic inside the interface (Java 9+); not visible to implementors |
| Abstract classes | Partial implementation — can have state, constructors, non-public members |
| Interface vs abstract class | One axis: interface = capability / role; abstract class = shared implementation |
| Functional interfaces | Single abstract method → usable as lambda / method reference |
| `@FunctionalInterface` | Compiler enforces exactly one abstract method |
| Built-in functional types | `Function`, `Predicate`, `Consumer`, `Supplier`, `BiFunction`, `UnaryOperator` |
| Composition | `andThen`, `compose`, `and`, `or`, `negate` — build pipelines without loops |
| Multiple interface inheritance | Diamond problem and how `default` resolution rules solve it |

---

## Interface vs Abstract Class Decision Tree

```
                  Do you need STATE (fields)?
                         │
              ┌──────────┴──────────┐
             YES                    NO
              │                     │
    Abstract class            Do you need CONSTRUCTORS
    (can have fields,         or non-public members?
     constructors)                  │
                           ┌────────┴────────┐
                          YES                NO
                           │                 │
                    Abstract class       Interface
                                     (multiple impl,
                                      default methods)
```

---

## Default Method Resolution (Diamond)

```
     Validator          Auditable
    default log()      default log()
         │                  │
         └────────┬──────────┘
              MyService
         // MUST override log() — compiler forces resolution
         // OR: Validator.super.log() / Auditable.super.log()
```

---

## Functional Interface Cheat Sheet

```
Supplier<T>           ()     → T          "produce a value"
Consumer<T>           T      → void        "consume a value"
Function<T,R>         T      → R           "transform"
Predicate<T>          T      → boolean     "test"
BiFunction<T,U,R>     (T,U)  → R           "combine two"
UnaryOperator<T>      T      → T           "transform same type"
BinaryOperator<T>     (T,T)  → T           "reduce two to one"
Runnable              ()     → void        "run a block"
```

---

## Composition Pipeline

```
Function<String, Integer> length   = String::length;
Function<Integer, Boolean> isEven  = n -> n % 2 == 0;
Function<String, Boolean> pipeline = length.andThen(isEven);

pipeline.apply("Hello")  // length=5, isEven(5)=false → false
pipeline.apply("Java")   // length=4, isEven(4)=true  → true
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `InterfaceFeatures.java` | `default`, `static`, `private` interface methods; diamond resolution; multiple implementation |
| `AbstractVsInterface.java` | When to use each; Template Method in abstract class; capability interfaces |
| `FunctionalInterfaces.java` | `@FunctionalInterface`, all built-in types, lambda/method-ref syntax, composition |
| `OrderPipeline.java` | Real pipeline: validation → enrichment → transformation → output using functional composition |

---

## Running

```bash
cd module-09-oop-interfaces
mvn test
```
