---
title: "15 — Functional Programming"
parent: "Phase 2 — Core APIs"
nav_order: 15
render_with_liquid: false
---

# Module 15 — Functional Programming: Optional & Method References
{: .no_toc }

**Goal:** Replace null checks and anonymous class boilerplate with clean, composable functional code using `Optional<T>`, method references, and the `java.util.function` toolkit.

---

## Table of Contents
{: .no_toc .text-delta }
1. TOC
{:toc}

---

## Why Functional Programming in Java?

Java has been functional-capable since Java 8. The core idea: **treat behaviour as data**. Pass functions as arguments, return them as results, compose them into pipelines. This eliminates whole categories of bugs:

- Null checks → `Optional<T>`
- Anonymous inner classes → lambdas and method references
- Repeated strategy classes → `Function`/`Predicate` values
- Mutable accumulator loops → stream pipelines

---

## Optional\<T\>

`Optional<T>` is a container that holds either a value or nothing. It forces you to handle the absent case at the type level.

### Creation

```java
Optional<String> present = Optional.of("hello");       // non-null only
Optional<String> maybe   = Optional.ofNullable(value); // null becomes empty()
Optional<String> empty   = Optional.empty();
```

**Never** use `Optional.get()` without `isPresent()`. Use the transformation methods instead.

### map / flatMap / filter

```java
// map: transform the value if present
Optional<Integer> len = Optional.of("hello").map(String::length); // Optional[5]

// flatMap: when the mapping function itself returns Optional
Optional<Integer> parsed = Optional.of("42").flatMap(OptionalDemo::parseIntSafe);

// filter: keep value only if predicate passes
Optional<Integer> even = Optional.of(4).filter(n -> n % 2 == 0); // present
Optional<Integer> odd  = Optional.of(3).filter(n -> n % 2 == 0); // empty
```

### Terminal operations

| Method | Behaviour |
|---|---|
| `orElse(T)` | Return value or default (default **always** evaluated) |
| `orElseGet(Supplier<T>)` | Return value or call supplier (lazy — only when empty) |
| `orElseThrow(Supplier<X>)` | Return value or throw exception |
| `ifPresent(Consumer<T>)` | Run consumer if present, nothing if empty |
| `ifPresentOrElse(Consumer, Runnable)` | One branch per case (Java 9+) |

**Prefer `orElseGet` over `orElse` for expensive defaults:**

```java
// BAD: new ArrayList() always constructed, even when opt is present
return opt.orElse(new ArrayList<>());

// GOOD: ArrayList only constructed when opt is empty
return opt.orElseGet(ArrayList::new);
```

### or / stream (Java 9+)

```java
// or: fallback Optional — result stays wrapped
Optional<String> result = primary.or(() -> fallback);

// stream: flatMap empties away in stream pipelines
List<Integer> numbers = inputs.stream()
    .map(OptionalDemo::parseIntSafe) // Stream<Optional<Integer>>
    .flatMap(Optional::stream)        // Stream<Integer>
    .collect(Collectors.toList());
```

### Chained pipeline pattern

```java
return Optional.ofNullable(userId)
               .filter(id -> !id.isBlank())
               .map(profiles::get)
               .map(String::trim)
               .filter(name -> !name.isEmpty())
               .map(String::toLowerCase)
               .orElse("anonymous");
```

No null checks. No early returns. No `NullPointerException`.

---

## Method References

A method reference is a compact lambda that delegates to an existing method. Four kinds:

```
Kind              Syntax                    Equivalent lambda
──────────────────────────────────────────────────────────────────
Static            ClassName::staticMethod   x -> ClassName.method(x)
Bound instance    instance::method          x -> instance.method(x)
Unbound instance  ClassName::method         (obj,x) -> obj.method(x)
Constructor       ClassName::new            args -> new ClassName(args)
```

### Static

```java
.map(Integer::parseInt)           // s -> Integer.parseInt(s)
Comparator<Integer> cmp = Integer::compare;
```

### Bound instance

```java
String prefix = "Hello";
Predicate<String> startsWith = prefix::startsWith; // receiver captured at creation
Consumer<String> print = System.out::println;
```

### Unbound instance

```java
Function<String, String> upper = String::toUpperCase; // receiver = first arg at call time
Comparator<String> byLen = Comparator.comparingInt(String::length);
BiFunction<String, String, Boolean> contains = String::contains;
```

### Constructor

```java
Supplier<StringBuilder>         sbNew  = StringBuilder::new;
Function<String, StringBuilder> sbFrom = StringBuilder::new;
IntFunction<int[]>              arr    = int[]::new;
```

---

## java.util.function — Core Interfaces

| Interface | Signature | Use case |
|---|---|---|
| `Function<T,R>` | `T -> R` | transform |
| `Predicate<T>` | `T -> boolean` | test/filter |
| `Consumer<T>` | `T -> void` | side-effect |
| `Supplier<T>` | `() -> T` | produce/defer |
| `BiFunction<T,U,R>` | `T,U -> R` | two-input transform |
| `UnaryOperator<T>` | `T -> T` | in-place transform |
| `BinaryOperator<T>` | `T,T -> T` | fold/combine |

Primitive specialisations (`IntFunction`, `LongPredicate`, `ToIntFunction`, …) avoid boxing overhead.

### Composition

```java
// Function.andThen
Function<String, String> trimUpper =
    ((Function<String,String>) String::trim).andThen(String::toUpperCase);

// Predicate.and / or / negate
Predicate<String> valid = Predicate.not(String::isBlank)
                                   .and(s -> s.length() >= 8);

// Consumer.andThen
Consumer<String> logAndStore = logger::log.andThen(store::add);
```

### Custom @FunctionalInterface

```java
@FunctionalInterface
interface Transformer<A, B> {
    B transform(A input);

    default <C> Transformer<A, C> andThen(Transformer<B, C> after) {
        return input -> after.transform(this.transform(input));
    }
}

Transformer<String, Integer> wordCount = s -> s.trim().split("\\s+").length;
Transformer<String, String>  report    = wordCount.andThen(n -> "words: " + n);
```

---

## Functional Patterns

### Currying and partial application

```java
Function<Integer, Function<Integer, Integer>> add = curry(Integer::sum);
Function<Integer, Integer> add5 = add.apply(5);  // first arg fixed
```

### Memoization

```java
Function<Integer, Integer> memoized = memoize(n -> expensiveCompute(n));
memoized.apply(10); // computed
memoized.apply(10); // from cache — fn not called again
```

### Strategy via functions

```java
// No interface, no classes — strategies are just Function values
Function<Order, Double> strategy = tier.equals("GOLD") ? twentyPct : tenPct;
double price = strategy.apply(order);
```

### Validation combinator

```java
Validator<String> minLen  = s -> s.length() >= 8 ? emptyList() : List.of("too short");
Validator<String> noSpace = s -> !s.contains(" ") ? emptyList() : List.of("no spaces");

List<String> errors = minLen.and(noSpace).validate(input); // collects ALL failures
```

### Lazy evaluation

```java
Lazy<Config> config = Lazy.of(() -> Config.loadFromDisk()); // not loaded yet
Config c = config.get(); // loaded once here, cached for all subsequent calls
```

---

## Source Files

| File | What it covers |
|---|---|
| `OptionalDemo.java` | Creation, map, flatMap, filter, orElse family, or, stream, pipelines |
| `MethodReferences.java` | All four kinds, Comparator composition, processing pipelines |
| `FunctionalInterfaces.java` | Function/Predicate/Consumer/Supplier composition, primitives, custom interface |
| `FunctionalPatterns.java` | Higher-order fns, currying, memoization, Strategy, Decorator, Validator, Lazy |

---

## Common Mistakes

{: .warning }
> **`orElse` eagerly evaluates its argument.** `opt.orElse(expensiveCall())` calls `expensiveCall()` even when `opt` is present. Use `orElseGet(() -> expensiveCall())` instead.

{: .warning }
> **Don't wrap `Optional` in collections or fields.** `Optional` is designed for return values only. Storing `Optional` in a `List` or as a class field is an anti-pattern.

{: .tip }
> **Unbound vs bound:** `String::toUpperCase` as `Function<String,String>` is **unbound** — the string is the receiver. `myString::toUpperCase` as `Supplier<String>` is **bound** — `myString` is captured. Same syntax, different semantics depending on the target functional interface.
