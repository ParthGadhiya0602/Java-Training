---
title: "20 — Annotations"
parent: "Phase 2 — Core APIs"
nav_order: 20
render_with_liquid: false
---

# Module 20 — Annotations
{: .no_toc }

<details open markdown="block">
  <summary>Table of contents</summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

---

## Overview

Annotations are metadata attached to code elements (classes, methods, fields,
parameters). They do not change program logic directly but are read by:

- The **compiler** (`@Override`, `@SuppressWarnings`)
- **Frameworks** at runtime via reflection (Spring, JUnit, JPA)
- **Build tools** / annotation processors at compile time (Lombok, MapStruct)

---

## Defining Custom Annotations

An annotation type is declared with `@interface`:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Author {
    String value();           // "value" is the conventional single-element name
    String date() default ""; // optional element with default
}
```

Usage:
```java
@Author("Alice")              // short form — element named "value" can be omitted
@Author(value = "Bob", date = "2024-01-01")  // explicit form
```

### Element types

| Type | Example default |
|---|---|
| `String` | `""` |
| `int` / `long` / `boolean` | `0`, `0L`, `false` |
| `Class<?>` | `Void.class` (sentinel for "not specified") |
| `enum` | `MyEnum.DEFAULT` |
| `Annotation` | another annotation literal |
| `T[]` (array) | `{}` |

---

## Retention Policies

```java
@Retention(RetentionPolicy.SOURCE)   // stripped by javac; e.g. @Override
@Retention(RetentionPolicy.CLASS)    // stored in .class but not loaded (default)
@Retention(RetentionPolicy.RUNTIME)  // available via reflection at runtime
```

Only `RUNTIME` annotations can be read with reflection.

---

## Target

Controls where an annotation may be placed:

```java
@Target({
    ElementType.TYPE,           // class, interface, enum, record
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.CONSTRUCTOR,
    ElementType.LOCAL_VARIABLE,
    ElementType.ANNOTATION_TYPE,
    ElementType.PACKAGE,
    ElementType.TYPE_PARAMETER,  // <T extends ...>
    ElementType.TYPE_USE,        // any type usage
    ElementType.MODULE,
    ElementType.RECORD_COMPONENT
})
```

### TYPE_USE example

`TYPE_USE` allows annotation on any type in the code:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
public @interface NonNull {}

// usage
@NonNull String name;
List<@NonNull String> items;
Object obj = (@NonNull Object) rawRef;
```

---

## Meta-Annotations

| Meta-annotation | Effect |
|---|---|
| `@Retention` | How long the annotation is kept |
| `@Target` | Where it can be placed |
| `@Documented` | Include in Javadoc |
| `@Inherited` | Subclasses inherit superclass annotation |
| `@Repeatable` | Same annotation may appear more than once |

---

## @Repeatable

To allow multiple occurrences of the same annotation, declare a container:

```java
// Step 1 — the repeatable annotation
@Repeatable(Tags.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Tag {
    String value();
}

// Step 2 — the container annotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Tags {
    Tag[] value();
}

// Usage
@Tag("api")
@Tag("public")
public class UserService { ... }

// Reading (getAnnotationsByType unwraps the container automatically)
Tag[] tags = UserService.class.getAnnotationsByType(Tag.class);
```

---

## @Inherited

When a class has an `@Inherited` annotation, subclasses automatically inherit it:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Component {
    String name() default "";
}

@Component(name = "baseRepo")
class BaseRepository { ... }

class UserRepository extends BaseRepository { ... }  // no @Component declared

// But...
UserRepository.class.isAnnotationPresent(Component.class);  // true!
```

Note: `@Inherited` only applies to class-level annotations, not methods or fields.

---

## Built-in Java Annotations

### @Override

```java
@Override
public String toString() { ... }  // compiler error if nothing to override
```

Without `@Override`, a typo in the method name silently creates a new method.

### @Deprecated

```java
@Deprecated(since = "2.0", forRemoval = true)
public String legacyFormat(int v) { ... }
```

- `since` — version when deprecated
- `forRemoval = true` — stronger signal; tools warn more aggressively

### @SuppressWarnings

```java
@SuppressWarnings("unchecked")    // suppress unchecked cast
@SuppressWarnings("deprecation")  // suppress deprecated API usage
@SuppressWarnings({"unchecked", "rawtypes"})  // multiple
```

Common names: `"unchecked"`, `"deprecation"`, `"rawtypes"`, `"unused"`, `"serial"`.

### @FunctionalInterface

```java
@FunctionalInterface
public interface Transformer<T, R> {
    R transform(T input);   // exactly one abstract method — compiler enforces this
}
```

### @SafeVarargs

Suppresses heap-pollution warning for generic varargs. Only valid on `final`,
`static`, `private` methods or constructors:

```java
@SafeVarargs
public static <T> List<T> listOf(T... items) {
    return List.of(items);   // safe: we don't write into the array
}
```

---

## Runtime Processing via Reflection

```java
// Class-level
Author a   = MyClass.class.getAnnotation(Author.class);
Tag[] tags = MyClass.class.getAnnotationsByType(Tag.class);  // handles @Repeatable
boolean b  = MyClass.class.isAnnotationPresent(Beta.class);
Annotation[] all = MyClass.class.getDeclaredAnnotations();

// Method-level
for (Method m : clazz.getDeclaredMethods()) {
    RequiresRoles rr = m.getAnnotation(RequiresRoles.class);
    if (rr != null) { /* enforce roles */ }
}

// Field-level
for (Field f : clazz.getDeclaredFields()) {
    if (f.isAnnotationPresent(Inject.class)) {
        f.setAccessible(true);
        f.set(obj, resolveInstance(f.getType()));
    }
}
```

### Typical framework patterns

| Pattern | How annotations help |
|---|---|
| Dependency injection | `@Inject` on fields → framework sets values via reflection |
| Access control | `@RequiresRoles` on methods → interceptor checks before call |
| Validation | `@NonNull`, `@Min` on fields → validator reads and checks |
| ORM | `@Table`, `@Column` on classes/fields → maps to DB schema |
| Test runners | `@Test`, `@BeforeEach` on methods → JUnit discovers and calls |

---

## Summary

| Concept | Annotation / API |
|---|---|
| Define annotation | `@interface` with `@Retention` + `@Target` |
| Available at runtime | `RetentionPolicy.RUNTIME` |
| Multiple occurrences | `@Repeatable` + container annotation |
| Subclass inheritance | `@Inherited` (class-level only) |
| Compiler check | `@Override`, `@FunctionalInterface` |
| Suppress warnings | `@SuppressWarnings("unchecked")` |
| Read at runtime | `getAnnotation()`, `getAnnotationsByType()`, `getDeclaredAnnotations()` |
| Field injection | `field.setAccessible(true); field.set(obj, value)` |
