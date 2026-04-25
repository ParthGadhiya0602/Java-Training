---
title: "14 — Streams API"
parent: "Phase 2 — Core APIs"
nav_order: 14
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-14-streams/src){: .btn .btn-outline }

# Module 14 — Streams API

## What You Will Learn

| Category | Operations |
|---|---|
| Creating streams | `Collection.stream()`, `Stream.of()`, `Stream.generate()`, `Stream.iterate()`, `IntStream.range()` |
| Intermediate (lazy) | `filter`, `map`, `flatMap`, `distinct`, `sorted`, `peek`, `limit`, `skip`, `mapToInt/Long/Double` |
| Terminal (eager) | `collect`, `forEach`, `count`, `reduce`, `findFirst/Any`, `anyMatch/allMatch/noneMatch`, `min/max`, `toArray` |
| Collectors | `toList`, `toSet`, `toMap`, `groupingBy`, `partitioningBy`, `joining`, `counting`, `summarizingInt`, `teeing` |
| Primitive streams | `IntStream`, `LongStream`, `DoubleStream` — `sum`, `average`, `range`, `rangeClosed` |

---

## The Pipeline Model

```
Source → [Intermediate ops (lazy)] → Terminal op (triggers execution)

list.stream()          // source
    .filter(...)       // lazy — nothing runs yet
    .map(...)          // lazy — nothing runs yet
    .collect(...)      // terminal — pipeline executes NOW
```

---

## Intermediate vs Terminal

```
INTERMEDIATE (return Stream<T>)    TERMINAL (return a value or void)
──────────────────────────────     ─────────────────────────────────
filter(Predicate)                  collect(Collector)
map(Function)                      forEach(Consumer)
flatMap(Function)                  count()
distinct()                         reduce(identity, BinaryOperator)
sorted([Comparator])               findFirst() / findAny()
peek(Consumer)                     anyMatch / allMatch / noneMatch
limit(long)                        min(Comparator) / max(Comparator)
skip(long)                         toList()   (Java 16+)
mapToInt/Long/Double               toArray()
```

---

## Collectors Cheat Sheet

```java
// Basic collectors
Collectors.toList()                      → List<T>
Collectors.toSet()                       → Set<T>
Collectors.toUnmodifiableList()          → unmodifiable List<T>
Collectors.joining(", ", "[", "]")       → String
Collectors.counting()                    → Long
Collectors.toMap(keyFn, valueFn)         → Map<K,V>

// Grouping
Collectors.groupingBy(classifier)        → Map<K, List<T>>
Collectors.groupingBy(classifier, downstream)
Collectors.partitioningBy(predicate)     → Map<Boolean, List<T>>

// Statistics
Collectors.summarizingInt(fn)            → IntSummaryStatistics
Collectors.averagingInt(fn)              → Double
Collectors.summingInt(fn)                → Integer

// Composing (Java 12+)
Collectors.teeing(c1, c2, merger)        → merges results of two collectors
```

---

## Source Files

| File | What it Demonstrates |
|---|---|
| `StreamBasics.java` | Creating streams, filter/map/sorted/limit/skip, terminal ops, `Optional` from streams |
| `CollectorsDeep.java` | `groupingBy`, `partitioningBy`, `toMap`, `joining`, `summarizingInt`, `teeing`, downstream collectors |
| `PrimitiveStreams.java` | `IntStream`/`LongStream`/`DoubleStream`, boxing/unboxing, `range`/`rangeClosed`, statistics |
| `StreamPatterns.java` | `flatMap`, lazy evaluation, `generate`/`iterate`, real-world pipelines |

---

## Running

```bash
cd module-14-streams
mvn test
```
{% endraw %}
