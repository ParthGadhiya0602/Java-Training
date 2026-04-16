---
title: "11 — Nested & Inner Classes"
parent: "Phase 1 — Fundamentals"
nav_order: 11
render_with_liquid: false
---

# Module 11 — Nested & Inner Classes

## What You Will Learn

| Kind | Keyword | Has outer `this`? | When to use |
|---|---|---|---|
| Static nested class | `static class Foo` inside a class | No | Logically grouped with outer; no outer-instance needed |
| Non-static inner class | `class Foo` inside a class | Yes | Tightly coupled to outer instance; rare in modern Java |
| Local class | `class Foo` inside a method | Yes (if non-static method) | One-off implementation local to a block |
| Anonymous class | `new Interface() { ... }` | Yes (if non-static context) | Single-use; largely replaced by lambdas in Java 8+ |

---

## Memory Model

```
Static Nested Class                   Inner (Non-static) Class
───────────────────                   ────────────────────────
  OuterClass                            OuterClass instance
  ├─ static fields                      ├─ fields
  └─ NestedClass (static)               └─ InnerClass (non-static)
       no link back to outer                  hidden field: outer$this
                                              can access outer.privateField
```

---

## The Four Kinds

```java
class Outer {
    // 1. Static nested — no outer instance needed
    static class StaticNested { }

    // 2. Inner (non-static) — carries a reference to Outer.this
    class Inner { void hi() { System.out.println(outerField); } }

    // 3. Local class — defined inside a method body
    void method() {
        class Local implements Runnable {
            public void run() { System.out.println("local"); }
        }
        new Local().run();
    }

    // 4. Anonymous class — instantiated inline, no name
    Runnable r = new Runnable() {
        public void run() { System.out.println("anon"); }
    };
}
```

---

## When Lambda Replaces Anonymous Class

```
Anonymous class is REQUIRED when:               Lambda is PREFERRED when:
  • Implementing an interface with              • Exactly one abstract method
    multiple abstract methods                  • No need to store state in fields
  • Need to hold multiple fields               • No need for super/this reference
  • Need to call super                         • Conciseness matters
  • Need to reference 'this' (the anon obj)

// Pre-Java 8 — anonymous class
Collections.sort(list, new Comparator<String>() {
    public int compare(String a, String b) { return a.compareTo(b); }
});

// Java 8+ — lambda (Comparator is @FunctionalInterface)
list.sort(String::compareTo);
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `StaticNestedDemo.java` | Static nested classes: Builder, Node (linked list), Entry (map) |
| `InnerClassDemo.java` | Non-static inner class: Iterator, event listener, outer access |
| `LocalAndAnonymous.java` | Local classes, anonymous classes, and when lambdas replace them |
| `NestedClassPatterns.java` | Real patterns: private static nested for encapsulation, builder, composite |

---

## Running

```bash
cd module-11-nested-classes
mvn test
```
