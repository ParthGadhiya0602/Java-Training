---
title: "21 — Reflection API"
parent: "Phase 2 — Core APIs"
nav_order: 21
render_with_liquid: false
---

# Module 21 — Reflection API
{: .no_toc }

<details open markdown="block">
  <summary>Table of contents</summary>
  {: .text-delta }
1. TOC
{:toc}
</details>

---

## Overview

The Reflection API (`java.lang.reflect`) lets code inspect and manipulate the
structure of other classes at runtime: read/write fields, invoke methods, create
instances, and generate proxy objects — all without knowing the types at compile time.

| Class | Purpose |
|---|---|
| `Class<T>` | Type metadata — the entry point |
| `Field` | Instance or static field |
| `Method` | Instance or static method |
| `Constructor<T>` | Constructor |
| `Parameter` | Parameter of a method/constructor |
| `Modifier` | Int bitmask of access flags |
| `Proxy` | Runtime-generated proxy that implements interfaces |

---

## Obtaining a Class Object

```java
// 1. Compile-time literal
Class<String> c1 = String.class;

// 2. Runtime type of an instance
Class<?> c2 = "hello".getClass();

// 3. Dynamic lookup by fully-qualified name
Class<?> c3 = Class.forName("java.lang.String");
```

---

## Inspecting Classes

```java
clazz.getSimpleName()          // "ArrayList"
clazz.getCanonicalName()       // "java.util.ArrayList"
clazz.getPackageName()         // "java.util"
clazz.getSuperclass()          // Class of the parent
clazz.getInterfaces()          // directly implemented interfaces
clazz.isInterface()            // true for interfaces
clazz.isEnum()                 // true for enums
clazz.isRecord()               // true for records (Java 16+)
Modifier.isAbstract(clazz.getModifiers())  // true for abstract classes
```

### getDeclared* vs get*

| Method | Returns |
|---|---|
| `getDeclaredFields()` | Own fields (any visibility; excludes inherited) |
| `getFields()` | Public fields of this class and all superclasses |
| `getDeclaredMethods()` | Own methods (any visibility; excludes inherited) |
| `getMethods()` | Public methods including inherited ones |
| `getDeclaredConstructors()` | All constructors of this class |

---

## Reading and Writing Fields

```java
Field field = clazz.getDeclaredField("secret");
field.setAccessible(true);     // bypass private access

Object value = field.get(obj);       // read instance field
field.set(obj, newValue);            // write instance field

Object val = field.get(null);        // read static field
field.set(null, newValue);           // write static field
```

`setAccessible(true)` bypasses `private`/`protected`. In modules it requires
an `opens` declaration or `--add-opens` JVM flag.

---

## Invoking Methods

```java
Method m = clazz.getDeclaredMethod("greet", String.class);
m.setAccessible(true);
Object result = m.invoke(obj, "Alice");   // instance method

Object result = m.invoke(null, "Alice");  // static method
```

`method.invoke()` wraps checked exceptions in `InvocationTargetException`:

```java
try {
    m.invoke(obj, args);
} catch (InvocationTargetException e) {
    throw e.getCause();   // unwrap the real exception
}
```

---

## Creating Instances

```java
// Via Constructor
Constructor<Person> ctor = Person.class.getDeclaredConstructor(String.class, int.class);
ctor.setAccessible(true);
Person p = ctor.newInstance("Alice", 30);

// No-arg shortcut
Person p2 = Person.class.getDeclaredConstructor().newInstance();
```

---

## Generic Type Introspection

Type erasure removes generic type parameters at runtime, but they are preserved
in class metadata for fields and method signatures:

```java
Field f = MyClass.class.getDeclaredField("items");
ParameterizedType pt = (ParameterizedType) f.getGenericType();
Class<?> typeArg = (Class<?>) pt.getActualTypeArguments()[0];  // e.g. String.class
```

For a typed subclass:

```java
class StringList extends ArrayList<String> {}

ParameterizedType pt = (ParameterizedType) StringList.class.getGenericSuperclass();
Class<?> typeArg = (Class<?>) pt.getActualTypeArguments()[0];  // String.class
```

---

## Dynamic Proxies

`java.lang.reflect.Proxy` generates a proxy class at runtime that implements
one or more interfaces. Every method call is dispatched to an `InvocationHandler`.

```java
Calculator proxy = (Calculator) Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    (proxyObj, method, args) -> {
        System.out.println("calling " + method.getName());
        return method.invoke(target, args);   // delegate to real object
    }
);
```

### Rules

- Proxy can only implement **interfaces**, not extend classes
- `Proxy.isProxyClass(obj.getClass())` — checks if an object is a proxy
- `Proxy.getInvocationHandler(proxy)` — retrieves the handler

### Common patterns

| Pattern | How the handler works |
|---|---|
| **Logging** | Record method name + args, then delegate |
| **Timing** | Capture `System.nanoTime()` before/after, then delegate |
| **Caching** | Return cached result if key (method + args) seen before |
| **Null guard** | Throw if any arg is null before delegating |
| **Retry** | Catch exceptions and retry up to N times |
| **Read-only** | Throw on setter methods; pass getters through |

### Generic proxy factory

```java
Calculator calc = (Calculator) Proxy.newProxyInstance(
    Calculator.class.getClassLoader(),
    new Class<?>[] { Calculator.class },
    (proxy, method, args) -> switch (method.getName()) {
        case "add"      -> (int) args[0] + (int) args[1];
        case "multiply" -> (int) args[0] * (int) args[1];
        default         -> "mock";
    }
);
```

---

## Practical Patterns

### Object ↔ Map mapper

```java
// Object → Map
Map<String, Object> map = new LinkedHashMap<>();
for (Field f : obj.getClass().getDeclaredFields()) {
    if (Modifier.isStatic(f.getModifiers())) continue;
    f.setAccessible(true);
    map.put(f.getName(), f.get(obj));
}

// Map → Object
for (Field f : obj.getClass().getDeclaredFields()) {
    if (!values.containsKey(f.getName())) continue;
    f.setAccessible(true);
    f.set(obj, values.get(f.getName()));
}
```

### Reflective toString / equals / hashCode

```java
// toString: ClassName{field1=val1, field2=val2}
// equals:   compare each field with Objects.equals
// hashCode: Objects.hash(field1, field2, ...)
```

Records do this automatically — prefer records over hand-rolled reflection-based
equals/hashCode.

### Plugin loader

```java
Class<?> pluginClass = Class.forName(className);
if (!expectedType.isAssignableFrom(pluginClass))
    throw new ClassCastException(...);
Constructor<?> ctor = pluginClass.getDeclaredConstructor();
ctor.setAccessible(true);
T plugin = (T) ctor.newInstance();
```

---

## Performance Considerations

| Action | Mitigation |
|---|---|
| Repeated `getDeclaredMethod` lookups | Cache `Method` / `Field` objects |
| `setAccessible(true)` on each call | Call once; the flag persists on the object |
| Proxy dispatch overhead | Profile first; rarely a bottleneck vs business logic |
| Type erasure for generics | Use `ParameterizedType` via field/method signatures |

---

## When to Use Reflection

**Good uses:**
- Dependency injection containers (Spring, Guice)
- Object-relational mappers (Hibernate, JPA)
- JSON/XML serialisation (Jackson, GSON)
- Test utilities and mocking (JUnit, Mockito)

**Avoid when:**
- You control the types at compile time — use generics or interfaces instead
- Performance is critical in a hot path
- You want compile-time safety

---

## Summary

| Task | API |
|---|---|
| Get class metadata | `clazz.getDeclaredFields/Methods/Constructors()` |
| Read/write field | `field.setAccessible(true); field.get/set(obj)` |
| Invoke method | `method.setAccessible(true); method.invoke(obj, args)` |
| Create instance | `ctor.setAccessible(true); ctor.newInstance(args)` |
| Dynamic proxy | `Proxy.newProxyInstance(loader, interfaces, handler)` |
| Generic type arg | `(ParameterizedType) field.getGenericType()` |
| Check proxy | `Proxy.isProxyClass(obj.getClass())` |
