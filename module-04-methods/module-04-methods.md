---
title: "04 — Methods"
parent: "Phase 1 — Fundamentals"
nav_order: 4
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-04-methods/src){: .btn .btn-outline }

# Module 04 — Methods

> **Phase:** Fundamentals | **Build tool:** Maven | **Java:** 21

---

## Table of Contents

1. [What is a Method?](#1-what-is-a-method)
2. [Method Anatomy](#2-method-anatomy)
3. [Return Types](#3-return-types)
4. [Static vs Instance Methods](#4-static-vs-instance-methods)
5. [Method Overloading](#5-method-overloading)
6. [Overload Resolution — How the Compiler Chooses](#6-overload-resolution--how-the-compiler-chooses)
7. [Varargs](#7-varargs)
8. [Pass-by-Value vs Pass-by-Reference](#8-pass-by-value-vs-pass-by-reference)
9. [The Call Stack](#9-the-call-stack)
10. [Recursion](#10-recursion)
11. [Recursion vs Iteration — When to Use Each](#11-recursion-vs-iteration--when-to-use-each)
12. [Practical Exercise — BankAccount](#12-practical-exercise--bankaccount)
13. [Exercises](#13-exercises)

---

## 1. What is a Method?

A **method** is a named block of code that performs a task. It can accept
inputs (parameters), execute statements, and optionally return an output.

Methods exist for two reasons:
1. **Reuse** — write the logic once, call it from anywhere
2. **Abstraction** — give a name to a complex operation so callers don't need
   to know *how* it works, only *what* it does

```
  Without methods (duplicated logic):            With methods (single definition):
  ┌─────────────────────────────┐                ┌────────────────────────────┐
  │  // in checkout             │                │  double tax(double amount) │
  │  double t = amt * 0.18;     │                │  { return amount * 0.18; } │
  │  total = amt + t;           │                └────────────┬───────────────┘
  │                             │                             │ called from:
  │  // in invoice              │                ┌────────────▼───────────────┐
  │  double t2 = amt2 * 0.18;   │                │  checkout: tax(amount)     │
  │  total2 = amt2 + t2;        │                │  invoice:  tax(amount)     │
  │                             │                │  report:   tax(amount)     │
  │  // in report               │                └────────────────────────────┘
  │  double t3 = amt3 * 0.18;   │
  │  total3 = amt3 + t3;        │     If the tax rate changes: update ONE place,
  └─────────────────────────────┘     not every place it was copied.
```

---

## 2. Method Anatomy

```
  access   return                    parameter list
  modifier type    name              ┌────────────────────────┐
  ┌──┴──┐ ┌──┴──┐ ┌─┴──┐            │                        │
  public  double  tax  (double amount, String currency)  {
                                                              ┐
      if (amount < 0)                                         │
          throw new IllegalArgumentException("...");          │
                                                              │  body
      double rate = currency.equals("USD") ? 0.10 : 0.18;    │
      return amount * rate;          ← return statement       │
  }                                                           ┘
```

| Part | Description |
|------|-------------|
| **Access modifier** | `public` / `protected` / (package-private) / `private` — who can call this method |
| **Return type** | The type of value the method produces. `void` means it returns nothing |
| **Method name** | camelCase by convention. Should be a verb: `calculate`, `find`, `validate` |
| **Parameter list** | Zero or more `type name` pairs, comma-separated |
| **Body** | The statements that execute when the method is called |
| **return** | Exits the method and optionally sends a value back to the caller |

### Method Signature

The **signature** is the method name + parameter types (order matters).
Return type is NOT part of the signature.

```java
double tax(double amount)                  // signature: tax(double)
double tax(double amount, String currency) // signature: tax(double, String)
// These are two different methods — they have different signatures (overloading)

double tax(double amount)                  // signature: tax(double)
int    tax(double amount)                  // COMPILE ERROR — same signature, different return type
```

---

## 3. Return Types

### void — No Return Value

```java
static void printSeparator(char c, int length) {
    for (int i = 0; i < length; i++) System.out.print(c);
    System.out.println();
    // no return statement needed (or just: return; to exit early)
}
```

### Returning a Value

```java
static double circleArea(double radius) {
    if (radius < 0)
        throw new IllegalArgumentException("Radius cannot be negative");
    return Math.PI * radius * radius;  // sends value back to caller
}

// Caller receives the value:
double area = circleArea(5.0);
```

### Early Return — Guard Clauses

Early returns make the "happy path" obvious by handling edge cases first:

```java
// Without early return — hard to follow
static String processOrder(Order order) {
    String result;
    if (order != null) {
        if (order.isValid()) {
            if (order.hasStock()) {
                result = "Processed: " + order.getId();
            } else {
                result = "Out of stock";
            }
        } else {
            result = "Invalid order";
        }
    } else {
        result = "Null order";
    }
    return result;
}

// With early return (guard clauses) — much cleaner
static String processOrder(Order order) {
    if (order == null)     return "Null order";
    if (!order.isValid())  return "Invalid order";
    if (!order.hasStock()) return "Out of stock";
    return "Processed: " + order.getId();  // happy path is now obvious
}
```

---

## 4. Static vs Instance Methods

### Static Methods

Belong to the **class**, not to any object. Called as `ClassName.method()`.
They have no `this` reference and cannot access instance fields.

```java
public class MathUtils {
    // static: no object state needed — just pure computation on inputs
    public static int add(int a, int b) { return a + b; }
    public static double square(double n) { return n * n; }
}

// Call without creating an object
int sum = MathUtils.add(3, 4);
```

Use static methods for:
- Utility/helper operations (stateless, pure functions)
- Factory methods (`Integer.valueOf()`, `List.of()`)
- Operations that don't logically belong to any single object

### Instance Methods

Belong to an **object**. They have access to `this` (the current object's state).

```java
public class Counter {
    private int count = 0;  // instance field — each object has its own

    public void increment() { this.count++; }    // modifies THIS object's count
    public int  getCount()  { return this.count; }
}

Counter c1 = new Counter();
Counter c2 = new Counter();
c1.increment(); c1.increment();
c2.increment();
System.out.println(c1.getCount()); // 2  — independent state
System.out.println(c2.getCount()); // 1
```

```
  Static vs Instance — memory model:

  Class definition (loaded once):
  ┌───────────────────────────────────────────┐
  │  Counter class                            │
  │  ┌─────────────────────────────────────┐ │
  │  │  static: (nothing here for Counter)  │ │
  │  ├─────────────────────────────────────┤ │
  │  │  increment() ← method CODE (shared) │ │
  │  │  getCount()  ← method CODE (shared) │ │
  │  └─────────────────────────────────────┘ │
  └───────────────────────────────────────────┘

  Two objects on the heap:
  ┌──────────────┐    ┌──────────────┐
  │  c1 (Counter)│    │  c2 (Counter)│
  │  count = 2   │    │  count = 1   │   ← each has its OWN count
  └──────────────┘    └──────────────┘
```

---

## 5. Method Overloading

**Overloading** means defining multiple methods with the **same name** but
**different parameter lists** (different types, different count, or different
order). The compiler picks the right one at compile time based on the arguments.

```java
// Three overloaded versions of 'log'
static void log(String message) {
    System.out.println("[INFO] " + message);
}

static void log(String message, String level) {
    System.out.println("[" + level + "] " + message);
}

static void log(String message, String level, Throwable cause) {
    System.out.println("[" + level + "] " + message + " | " + cause.getMessage());
}

// Caller picks the right one naturally:
log("Server started");                          // calls version 1
log("Disk usage high", "WARN");                // calls version 2
log("Connection failed", "ERROR", exception);  // calls version 3
```

### What Makes Overloads Distinct

| Distinguishes overloads? | Example |
|--------------------------|---------|
| Number of parameters | `add(int a)` vs `add(int a, int b)` |
| Type of parameters | `print(int n)` vs `print(double n)` |
| Order of parameter types | `copy(String src, int n)` vs `copy(int n, String src)` |
| Return type alone | NOT ENOUGH — compile error |
| Parameter names alone | NOT ENOUGH — compile error |

```java
// COMPILE ERROR: same signature (name + param types), different return type only
int    getValue(String key) { ... }
String getValue(String key) { ... }  // ← error
```

---

## 6. Overload Resolution — How the Compiler Chooses

When you call an overloaded method, the compiler follows a strict priority order
to find the best match. Understanding this prevents subtle bugs.

```
  Priority order (highest to lowest):

  1. Exact match          — types match exactly
  2. Widening             — compiler promotes type (int → long, float → double)
  3. Autoboxing           — primitive ↔ wrapper (int → Integer)
  4. Varargs              — matches ... parameter last
```

```java
static void show(int n)     { System.out.println("int: "     + n); }
static void show(long n)    { System.out.println("long: "    + n); }
static void show(Integer n) { System.out.println("Integer: " + n); }
static void show(int... ns) { System.out.println("varargs"); }

byte b = 10;
show(b);       // → int: 10    (widening: byte → int, exact match wins over long)
show(10);      // → int: 10    (exact match)
show(10L);     // → long: 10   (exact match)

Integer x = 5;
show(x);       // → Integer: 5 (exact match — autoboxing not needed)
show(5);       // → int: 5     (exact match wins over autoboxing to Integer)
```

**The widening trap:**

```java
static void process(long n)   { System.out.println("long");   }
static void process(float n)  { System.out.println("float");  }
static void process(double n) { System.out.println("double"); }

int i = 5;
process(i);    // → long: widening int→long is preferred over int→float
               // (widening integer type before widening to floating point)
```

---

## 7. Varargs

**Varargs** (variable-length arguments) lets a method accept **any number** of
arguments of the same type. Declared with `...` after the type.

```java
static int sum(int... numbers) {
    // 'numbers' is just an int[] inside the method body
    int total = 0;
    for (int n : numbers) total += n;
    return total;
}

// Can be called with 0, 1, 2, or any number of ints:
sum()           // → 0     (empty array passed)
sum(5)          // → 5
sum(1, 2, 3)    // → 6
sum(10, 20, 30, 40, 50) // → 150

// Can also explicitly pass an array:
int[] data = {3, 6, 9};
sum(data)       // → 18
```

### Rules for Varargs

```java
// 1. Varargs must be the LAST parameter
static void log(String level, String... messages) { ... }  // OK
// static void log(String... messages, String level) { ... }   // COMPILE ERROR

// 2. Only ONE varargs parameter per method
// static void bad(int... a, String... b) { ... }   // COMPILE ERROR

// 3. Null is valid and becomes a null array — guard against it
static void safeLog(String... messages) {
    if (messages == null) return;   // caller passed null explicitly
    for (String m : messages) System.out.println(m);
}
```

### Varargs and Overloading — A Subtle Trap

```java
static void display(String s)      { System.out.println("String:  " + s); }
static void display(String... ss)  { System.out.println("varargs: " + ss.length); }

display("hello");    // → String: hello   (exact match wins over varargs)
display("a", "b");   // → varargs: 2      (only varargs fits)
display();           // → varargs: 0      (only varargs fits empty call)
```

---

## 8. Pass-by-Value vs Pass-by-Reference

This is one of the most misunderstood topics in Java.

**Java is ALWAYS pass-by-value.**

The confusion arises because the "value" passed for an object is a **copy of
the reference** (memory address), not a copy of the object itself.

### Primitives — A True Copy

```java
static void tryToDouble(int x) {
    x = x * 2;    // modifies the LOCAL copy of x
    System.out.println("Inside: " + x);  // 20
}

int n = 10;
tryToDouble(n);
System.out.println("Outside: " + n);  // still 10 — original unchanged
```

```
  STACK before call:          STACK during tryToDouble:
  ┌────────────┐              ┌────────────────────────┐
  │ n = 10     │              │ n = 10  (original)     │
  └────────────┘              │ x = 10  (copy of n)    │
                              └────────────────────────┘
                                          │
                                     x = x * 2
                                          │
                              ┌────────────────────────┐
                              │ n = 10  (unchanged)    │
                              │ x = 20  (local copy)   │
                              └────────────────────────┘
  After return: x is gone.  n is still 10.
```

### Objects — The Reference is Copied, Not the Object

```java
static void appendBang(StringBuilder sb) {
    sb.append("!");   // modifies the object that 'sb' points to
}

StringBuilder msg = new StringBuilder("Hello");
appendBang(msg);
System.out.println(msg);  // "Hello!" — the object WAS modified
```

```
  HEAP:                           STACK:
  ┌────────────────────┐          ┌────────────────┐
  │ StringBuilder      │◄─────────│ msg (ref)      │  (original)
  │ value = "Hello"    │◄─────────│ sb  (copy ref) │  (inside appendBang)
  └────────────────────┘          └────────────────┘
        │
   sb.append("!")
        │
  ┌────────────────────┐
  │ StringBuilder      │   ← both msg and sb point to the SAME object
  │ value = "Hello!"   │     so the change IS visible to the caller
  └────────────────────┘
```

### Reassigning the Reference — Not Visible to Caller

```java
static void tryToReplace(StringBuilder sb) {
    sb = new StringBuilder("Replaced");  // only changes the LOCAL reference 'sb'
    // The original 'msg' reference in the caller still points to the old object
}

StringBuilder msg = new StringBuilder("Original");
tryToReplace(msg);
System.out.println(msg);  // "Original" — the replacement is invisible to caller
```

```
  Before call:         During tryToReplace:          After return:
  ┌──────────┐         ┌──────────┐                  ┌──────────┐
  │msg ──────┼────►    │msg ──────┼──► "Original"    │msg ──────┼──► "Original"
  └──────────┘         │sb  ──────┼──► "Original"    └──────────┘
                       └────┬─────┘
                      sb = new StringBuilder(...)
                       ┌────▼─────┐
                       │sb  ──────┼──► "Replaced"   (new object, msg unaffected)
                       └──────────┘
```

### Summary

```
  ┌──────────────────────────────────────────────────────────────┐
  │  Passed type         What is copied       Caller sees change?│
  ├──────────────────────────────────────────────────────────────┤
  │  Primitive (int)     the value itself      No               │
  │  Object reference    the reference (addr)  YES if object    │
  │                                            mutated via ref  │
  │  Object reference    the reference (addr)  No if ref is     │
  │  (reassigned)                              reassigned       │
  └──────────────────────────────────────────────────────────────┘
```

---

## 9. The Call Stack

Every method call pushes a **stack frame** onto the call stack.
Each frame holds: local variables, parameters, and the return address.
When a method returns, its frame is popped and the previous frame resumes.

```
  main() calls factorial(4), which calls factorial(3), ...

  CALL STACK (grows downward):

  ┌──────────────────────────┐  ← top of stack (most recent call)
  │  factorial(n=1)          │
  │  return address → f(2)   │
  ├──────────────────────────┤
  │  factorial(n=2)          │
  │  return address → f(3)   │
  ├──────────────────────────┤
  │  factorial(n=3)          │
  │  return address → f(4)   │
  ├──────────────────────────┤
  │  factorial(n=4)          │
  │  return address → main   │
  ├──────────────────────────┤
  │  main()                  │
  └──────────────────────────┘  ← bottom of stack

  As each call returns:
  factorial(1) returns 1  → frame popped
  factorial(2) returns 2  → frame popped
  factorial(3) returns 6  → frame popped
  factorial(4) returns 24 → frame popped
  main() receives 24
```

### StackOverflowError

If recursion has no base case (or too many frames), the stack runs out of memory:

```java
static int infinite(int n) {
    return infinite(n + 1);  // no base case — stack grows forever
}
// infinite(0) → StackOverflowError after ~10,000 frames (JVM default stack size)
```

The default stack size is ~512KB–1MB. Each frame is typically a few hundred bytes.
This limits recursion depth to roughly **1,000–10,000** levels, depending on
frame size.

---

## 10. Recursion

A method is **recursive** if it calls itself. Every valid recursive method has:

1. **Base case** — a condition where it returns without calling itself (stops recursion)
2. **Recursive case** — calls itself with a **smaller/simpler** input (makes progress)

```
  Recursive definition of factorial:

  factorial(n) = 1                      if n == 0  ← base case
               = n * factorial(n - 1)   if n > 0   ← recursive case
                        └──── smaller subproblem
```

```java
static long factorial(int n) {
    if (n < 0) throw new IllegalArgumentException("n must be >= 0");
    if (n == 0) return 1;              // base case: stop here
    return n * factorial(n - 1);      // recursive case: delegate smaller problem
}
```

### Fibonacci — Two Recursive Calls

```java
// Naive recursive Fibonacci — exponential time O(2^n), DO NOT use in production
static long fibNaive(int n) {
    if (n <= 1) return n;             // base cases: fib(0)=0, fib(1)=1
    return fibNaive(n - 1) + fibNaive(n - 2);  // two recursive calls
}
// fibNaive(40) makes ~2 billion calls — extremely slow

// With memoization (cache already-computed results) — O(n) time
static long fibMemo(int n, long[] cache) {
    if (n <= 1) return n;
    if (cache[n] != 0) return cache[n];   // return cached result
    cache[n] = fibMemo(n - 1, cache) + fibMemo(n - 2, cache);
    return cache[n];
}
```

### Binary Search — Divide and Conquer Recursion

```java
// Array must be sorted. Returns index of target, or -1 if not found.
static int binarySearch(int[] arr, int target, int low, int high) {
    if (low > high) return -1;          // base case: search space exhausted

    int mid = low + (high - low) / 2;  // avoid (low+high)/2 — integer overflow risk
    if (arr[mid] == target) return mid; // base case: found it

    if (arr[mid] < target)
        return binarySearch(arr, target, mid + 1, high); // search right half
    else
        return binarySearch(arr, target, low, mid - 1);  // search left half
}
```

```
  binarySearch([1,3,5,7,9,11,13,15], target=11):

  [1, 3, 5, 7, | 9, 11, 13, 15]  → mid=3, arr[3]=7 < 11 → search right
                [9, 11, | 13, 15] → mid=5, arr[5]=11 = 11 → FOUND at index 5
```

---

## 11. Recursion vs Iteration — When to Use Each

```
  ┌─────────────────────────────────────────────────────────────────┐
  │  Use RECURSION when:                                            │
  │  - Problem naturally breaks into identical smaller subproblems  │
  │  - Working with tree or graph structures                        │
  │  - Divide-and-conquer algorithms (merge sort, quicksort)        │
  │  - Problem depth is bounded and not very large (<1000)          │
  │                                                                 │
  │  Use ITERATION when:                                            │
  │  - Simple counting, scanning, or accumulation                   │
  │  - Input size could be large (deep recursion → StackOverflow)   │
  │  - Performance is critical (no stack frame overhead)            │
  │  - Tail-recursive logic (Java doesn't optimize tail calls)      │
  └─────────────────────────────────────────────────────────────────┘

  Trade-off summary:
  ┌────────────────┬──────────────────────────┬──────────────────────┐
  │                │  Recursion               │  Iteration           │
  ├────────────────┼──────────────────────────┼──────────────────────┤
  │  Readability   │  Cleaner for tree/graph  │  Cleaner for linear  │
  │  Performance   │  Stack overhead          │  No overhead         │
  │  Stack risk    │  StackOverflow possible  │  None                │
  │  Debuggability │  Harder to trace         │  Easier to trace     │
  └────────────────┴──────────────────────────┴──────────────────────┘
```

> **Java does NOT optimize tail recursion.** Even if your recursive call is
> the very last statement, Java still creates a new stack frame. In languages
> like Kotlin, Scala, and Haskell, `@TailRec` or equivalent causes the compiler
> to convert tail recursion to a loop automatically. In Java, do it yourself.

---

## 12. Practical Exercise

### Files in This Module

| File | What it demonstrates |
|------|----------------------|
| `MethodBasics.java` | Anatomy, static vs instance, early return, guard clauses |
| `OverloadingDemo.java` | Overloading, overload resolution, widening + autoboxing traps |
| `VarargsDemo.java` | Varargs, varargs + overloading, null safety |
| `PassByValueDemo.java` | Primitive copy, object mutation, reference reassignment |
| `RecursionDemo.java` | Factorial, Fibonacci (naive + memoized), binary search, power set |
| `BankAccount.java` | Practical exercise — ties all concepts together |

### BankAccount — What it Demonstrates

A `BankAccount` class that uses:
- Instance methods for account operations vs static factory/utility methods
- Overloaded `deposit()` and `withdraw()` (int, double, String amount)
- Varargs `transferAll(BankAccount... accounts)` — distribute balance across accounts
- Recursion for compound interest calculation over time periods
- Pass-by-value demonstration via a `transfer()` method
- Guard clauses with early return for input validation

**Run:**
```bash
cd module-04-methods
mvn compile exec:java -Dexec.mainClass="com.javatraining.methods.BankAccount"
```

**Test:**
```bash
mvn test
```

---

## 13. Exercises

**1. Overloading — Volume Calculator**
Write overloaded `volume()` methods for:
- `volume(double side)` — cube
- `volume(double length, double width, double height)` — cuboid
- `volume(double radius)` — this conflicts with cube! How do you resolve it?
  (Hint: use a different name, or a wrapper type — discuss the design limitation)

**2. Varargs — Statistics**
Write `stats(double... values)` that returns a record containing
`min`, `max`, `sum`, and `average`. Handle the empty case.

**3. Pass-by-value**
Predict the output of:
```java
static void modify(int[] arr, int scalar) {
    arr[0] = arr[0] * scalar;   // (A)
    scalar = 99;                 // (B)
    arr = new int[]{100, 200};   // (C)
}
int[] data = {5, 10, 15};
modify(data, 3);
System.out.println(data[0]);   // what does this print?
System.out.println(data[2]);   // what does this print?
```
Explain *why* for each line: (A), (B), (C).

**4. Recursion — Tower of Hanoi**
Write `hanoi(int n, String from, String to, String via)` that prints the
moves to solve the Tower of Hanoi for `n` disks. For `n=3` the output
should show 7 moves.

**5. Recursion → Iteration**
Convert this recursive method to an iterative one:
```java
static int sumDigits(int n) {
    if (n < 10) return n;
    return (n % 10) + sumDigits(n / 10);
}
```

---

## Next

[Module 05 — Arrays, Strings & Regex](../module-05-arrays-strings/)
