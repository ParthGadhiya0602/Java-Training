---
title: "12 — Generics"
parent: "Phase 2 — Core APIs"
nav_order: 12
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-12-generics/src){: .btn .btn-outline }

# Module 12 — Generics

## What You Will Learn

| Concept | Summary |
|---|---|
| Generic classes & methods | `class Pair<A,B>`, `<T> T pick(T[] arr)` |
| Bounded type parameters | `<T extends Comparable<T>>`, `<T extends Number & Cloneable>` |
| Wildcards | `?`, `? extends T` (upper-bounded), `? super T` (lower-bounded) |
| PECS | Producer Extends, Consumer Super — when to use which wildcard |
| Type erasure | What the JVM really sees; why `new T()` is illegal |
| Generic patterns | Result<T>, generic cache, generic pipeline, type-safe heterogeneous container |

---

## Why Generics?

```
Without generics                  With generics
──────────────────                ──────────────
List list = new ArrayList();      List<String> list = new ArrayList<>();
list.add("hello");                list.add("hello");
String s = (String) list.get(0);  String s = list.get(0);  // no cast
// ClassCastException at runtime  // type error caught at compile time
```

---

## Bounded Type Parameters

```java
// Upper bound — T must be Comparable to itself
<T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
}

// Multiple bounds (class must come first)
<T extends Number & Comparable<T>> double sum(List<T> list) { ... }
```

---

## Wildcards — PECS Rule

```
PECS: Producer Extends, Consumer Super

void copy(List<? extends Number> src,    // PRODUCER — you only READ from it
          List<? super   Number> dst) {  // CONSUMER — you only WRITE to it
    for (Number n : src) dst.add(n);
}

Unbounded  ?            — read as Object; cannot write (except null)
Upper      ? extends T  — safe to READ as T; cannot write
Lower      ? super T    — safe to WRITE T; can only read as Object
```

---

## Type Erasure

```
Source code                       After erasure (what JVM sees)
───────────────────               ─────────────────────────────
List<String>                  →   List
Pair<Integer, String>         →   Pair
<T extends Comparable<T>>     →   Comparable  (leftmost bound)

Consequences:
  • Cannot do: new T(), new T[n], instanceof List<String>
  • Can do:    instanceof List<?>,  (T) value  (unchecked cast)
  • Generic type info IS preserved in class/method signatures (reflection)
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `GenericClasses.java` | Generic classes, generic methods, bounded type params, multiple bounds |
| `Wildcards.java` | All three wildcard forms, PECS in action, unbounded wildcards |
| `TypeErasure.java` | Erasure effects, bridge methods, `@SuppressWarnings("unchecked")`, Class tokens |
| `GenericPatterns.java` | Result<T>, generic LRU cache, type-safe heterogeneous container, generic pipeline |

---

## Running

```bash
cd module-12-generics
mvn test
```
{% endraw %}
