---
title: "22 - Modern Java (9–21)"
parent: "Phase 2 - Core APIs"
nav_order: 22
render_with_liquid: false
---

{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-22-modern-java/src){: .btn .btn-outline }

# Module 22 - Modern Java (Java 9–21)

{: .no_toc }

<details open markdown="block">
  <summary>Table of contents</summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

---

## Records (Java 16)

A record is a transparent, immutable data carrier. The compiler auto-generates
accessors, `equals`, `hashCode`, and `toString`.

```java
public record Point(double x, double y) { }

Point p = new Point(3.0, 4.0);
p.x();           // accessor - NOT getX()
p.y();
p.equals(...)    // value-based equality
p.toString()     // "Point[x=3.0, y=4.0]"
```

### What records can do

```java
public record Range(int min, int max) {
    // Compact constructor - validation / normalisation
    public Range {
        if (min > max) throw new IllegalArgumentException("min > max");
    }

    // Instance methods
    public boolean contains(int v) { return v >= min && v <= max; }

    // Static members
    public static Range unbounded() { return new Range(Integer.MIN_VALUE, Integer.MAX_VALUE); }
}
```

### Canonical constructor override

```java
public record Person(String name, int age) {
    public Person(String name, int age) {   // must assign all components
        this.name = name.strip();
        this.age  = age;
        if (age < 0) throw new IllegalArgumentException("age must be >= 0");
    }
}
```

### Generic records

```java
public record Pair<A, B>(A first, B second) {
    public Pair<B, A> swap() { return new Pair<>(second, first); }
}
```

### Records implement interfaces

```java
public interface Shape { double area(); }

public record Circle(double radius) implements Shape {
    @Override public double area() { return Math.PI * radius * radius; }
}
```

### What records cannot do

- Extend another class (they implicitly extend `java.lang.Record`)
- Declare instance fields outside the header
- Be abstract or mutable

---

## Pattern Matching

### instanceof pattern variable (Java 16)

```java
// Before
if (obj instanceof String) {
    String s = (String) obj;  // explicit cast
    return s.length();
}

// After - pattern variable
if (obj instanceof String s) {
    return s.length();        // s is in scope and already cast
}

// In compound condition - s is in scope for the whole &&-chain
if (obj instanceof String s && s.length() > 10) { ... }
```

### Switch expression (Java 14)

```java
// Arrow-style - no fall-through, returns value
String result = switch (day) {
    case "SAT", "SUN" -> "weekend";
    case "MON"        -> "weekday";
    default           -> throw new IllegalArgumentException(day);
};

// With yield (multi-statement arm)
int days = switch (month) {
    case 2 -> {
        boolean leap = year % 4 == 0;
        yield leap ? 29 : 28;
    }
    default -> 30;
};
```

### Sealed classes (Java 17)

Sealed classes restrict which classes may extend or implement them.
The compiler knows the complete set of subtypes → exhaustive switch without `default`:

```java
public sealed interface Expr
        permits Num, Add, Mul, Neg {}

public record Num(double value) implements Expr {}
public record Add(Expr left, Expr right) implements Expr {}
public record Mul(Expr left, Expr right) implements Expr {}
public record Neg(Expr expr) implements Expr {}
```

### Switch with type patterns + record patterns (Java 21)

```java
double eval(Expr expr) {
    return switch (expr) {
        case Num(double v)       -> v;
        case Add(Expr l, Expr r) -> eval(l) + eval(r);
        case Mul(Expr l, Expr r) -> eval(l) * eval(r);
        case Neg(Expr e)         -> -eval(e);
    };
}
```

### Guarded patterns (Java 21)

```java
String classify(Object obj) {
    return switch (obj) {
        case null                       -> "null";
        case Integer i when i < 0       -> "negative: " + i;
        case Integer i                  -> "non-negative: " + i;
        case String s when s.isBlank()  -> "blank string";
        case String s                   -> "string: " + s;
        default                         -> "other";
    };
}
```

### Nested record patterns

```java
record Point(int x, int y) {}
record Line(Point start, Point end) {}

switch (obj) {
    case Line(Point(int x1, int y1), Point(int x2, int y2)) ->
        "(%d,%d) → (%d,%d)".formatted(x1, y1, x2, y2);
}
```

---

## String API (Java 11–12)

```java
"  ".isBlank()              // true - Unicode whitespace aware
"  hello  ".strip()         // "hello" - Unicode whitespace aware
"  hello  ".stripLeading()  // "hello  "
"  hello  ".stripTrailing() // "  hello"
"ab".repeat(3)              // "ababab"
"a\nb\nc".lines()           // Stream<String>: a, b, c
"hello".indent(4)           // "    hello\n" (normalises line endings)
"  hi  ".transform(String::strip)  // "hi" - fluent, apply any Function<String,R>
```

---

## Text Blocks (Java 15)

```java
String json = """
        {
            "name": "%s",
            "age": %d
        }
        """.formatted(name, age);
```

- Start: `"""` followed by a newline
- End: `"""` on its own line sets the left margin
- Common leading whitespace is stripped automatically
- `\` at line end = line continuation (no newline in output)
- `\s` = trailing space (prevents IDE trimming)

---

## Immutable Collection Factories (Java 9)

```java
List.of("a", "b", "c")             // unmodifiable
Set.of(1, 2, 3)                     // unmodifiable, no duplicates
Map.of("k1", 1, "k2", 2)           // unmodifiable, up to 10 entries
Map.ofEntries(Map.entry("k", "v"))  // for > 10 entries

List.copyOf(existingList)           // defensive unmodifiable copy
Map.copyOf(existingMap)
```

---

## Optional Additions (Java 9–11)

```java
// or() - fallback to another Optional
opt.or(() -> fallback)

// ifPresentOrElse() - handle both branches
opt.ifPresentOrElse(v -> use(v), () -> handleAbsent());

// stream() - bridge into Stream pipelines
opts.stream().flatMap(Optional::stream)  // filters and unwraps in one step

// isEmpty() - explicit empty check (Java 11)
opt.isEmpty()
```

---

## Stream Additions (Java 9 / 16)

```java
// takeWhile - stops at first false (ordered streams)
stream.takeWhile(n -> n < 10)

// dropWhile - skips while true (ordered streams)
stream.dropWhile(n -> n < 10)

// iterate with termination condition (replaces iterate + limit)
Stream.iterate(0, n -> n < 100, n -> n + 1)

// ofNullable - empty stream for null, one-element stream otherwise
Stream.ofNullable(possiblyNullValue)

// toList() - Java 16, shorter than collect(Collectors.toList())
stream.toList()
```

---

## var - Local Variable Type Inference (Java 10)

```java
var list   = new ArrayList<String>();    // inferred as ArrayList<String>
var map    = new HashMap<String, List<Integer>>();
var entry  = map.entrySet().iterator().next();

for (var item : list) { ... }  // also works in for-each
```

`var` is a compile-time feature - the type is fixed. It does NOT make Java dynamic.

**When to use:** complex generic types where the type is obvious from the RHS.  
**Avoid:** when it harms readability (e.g. `var x = process(data)` - what is x?).

---

## Sequenced Collections (Java 21)

New interfaces `SequencedCollection` and `SequencedMap` add order-aware methods
to `List`, `Deque`, `LinkedHashSet`, `LinkedHashMap`, etc.:

```java
list.getFirst()      // first element
list.getLast()       // last element
list.addFirst(e)     // insert at front
list.addLast(e)      // insert at back
list.reversed()      // reversed view (no copy)

map.firstEntry()     // Map.Entry for the first key
map.lastEntry()
map.reversed()       // reversed view
```

---

## Summary by Version

| Version | Key addition                                                                                                   |
| ------- | -------------------------------------------------------------------------------------------------------------- |
| Java 9  | `List/Set/Map.of()`, `Optional.or/ifPresentOrElse/stream`, `Stream.takeWhile/dropWhile/iterate/ofNullable`     |
| Java 10 | `var`, `List/Map/Set.copyOf()`                                                                                 |
| Java 11 | `String.isBlank/strip/lines/repeat`, `Optional.isEmpty`                                                        |
| Java 12 | `String.indent/transform`                                                                                      |
| Java 14 | Switch expressions (final)                                                                                     |
| Java 15 | Text blocks (final)                                                                                            |
| Java 16 | Records (final), `instanceof` patterns (final), `Stream.toList()`                                              |
| Java 17 | Sealed classes (final)                                                                                         |
| Java 21 | Pattern matching for switch (final), record patterns, guarded patterns, sequenced collections, virtual threads |

{% endraw %}
