---
title: "02 — Java Basics"
parent: "Phase 1 — Fundamentals"
nav_order: 2
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-02-java-basics/src){: .btn .btn-outline }

# Module 02 — Java Basics

> **Phase:** Fundamentals | **Build tool:** Maven | **Java:** 21

---

## Table of Contents

1. [How Java Code Runs](#1-how-java-code-runs)
2. [Primitive Types](#2-primitive-types)
3. [Reference Types & the String Pool](#3-reference-types--the-string-pool)
4. [Type Casting](#4-type-casting)
5. [Autoboxing & the Integer Cache Trap](#5-autoboxing--the-integer-cache-trap)
6. [Operators](#6-operators)
7. [Operator Precedence](#7-operator-precedence)
8. [var — Local Type Inference](#8-var--local-type-inference)
9. [BigDecimal — Why double Fails for Money](#9-bigdecimal--why-double-fails-for-money)
10. [Practical Exercise — Expense Calculator](#10-practical-exercise--expense-calculator)
11. [Exercises](#11-exercises)

---

## 1. How Java Code Runs

Before writing any Java, you need a mental model of what happens when you run it.

```
  You write        Compiler          JVM runs
  .java file  ──►  javac  ──►  .class file (bytecode)  ──►  Output
                              (platform-neutral)
```

**Three-step process:**

1. `javac Hello.java` — The Java compiler reads your source code and produces
   **bytecode** (`.class` file). Bytecode is NOT machine code — it is an
   intermediate format that no real CPU understands natively.

2. The **JVM (Java Virtual Machine)** reads the bytecode and either interprets
   it or compiles it further to native machine code via the **JIT compiler**.

3. The JVM is what makes Java "write once, run anywhere" — the same `.class`
   file runs on Windows, macOS, and Linux as long as a JVM is installed.

```
  ┌──────────────────────────────────────────────────────────┐
  │                         JVM                              │
  │                                                          │
  │  .class ──► Class Loader ──► Bytecode Verifier          │
  │                                    │                     │
  │                          ┌─────────▼──────────┐         │
  │                          │  Execution Engine   │         │
  │                          │  - Interpreter      │         │
  │                          │  - JIT Compiler     │         │
  │                          └─────────────────────┘         │
  │                                    │                     │
  │              ┌─────────────────────▼───────────────┐    │
  │              │         Runtime Data Areas            │    │
  │              │  Stack | Heap | Method Area | PC Reg  │    │
  │              └─────────────────────────────────────-─┘    │
  └──────────────────────────────────────────────────────────┘
```

This JVM structure becomes very important in Module 16 (JVM Internals).
For now just know: **Stack stores method calls and primitives.
Heap stores objects.**

---

## 2. Primitive Types

Java has exactly **8 primitive types**. They are not objects — they have no
methods, no null value, and live directly on the stack.

### The 8 Primitives

| Type | Size | Range | Default | Use for |
|------|------|-------|---------|---------|
| `byte` | 8-bit | -128 to 127 | 0 | Raw binary data, file I/O |
| `short` | 16-bit | -32,768 to 32,767 | 0 | Rarely used directly |
| `int` | 32-bit | -2.1B to 2.1B | 0 | Default choice for integers |
| `long` | 64-bit | -9.2 × 10¹⁸ to 9.2 × 10¹⁸ | 0L | IDs, timestamps, large counts |
| `float` | 32-bit | ~7 decimal digits | 0.0f | Graphics, rarely in business logic |
| `double` | 64-bit | ~15 decimal digits | 0.0 | Default choice for decimals |
| `char` | 16-bit | 0 to 65,535 (Unicode) | '\u0000' | A single character |
| `boolean` | 1-bit* | true / false | false | Flags, conditions |

> *`boolean` is 1-bit logically, but JVMs typically store it as 1 byte or 4 bytes
> depending on context (array vs local variable).

### Declaring and Initializing

```java
// Declaration only — field gets default value, local variable does NOT
int count;           // local var: COMPILE ERROR if used before init
                     // class field: default is 0

// Declaration + initialization
int age = 25;
long userId = 1_000_000_001L;   // L suffix required for long literals
double price = 99.99;           // default decimal type is double
float tax = 0.18f;              // f suffix required for float literals
char grade = 'A';               // single quotes for char
boolean active = true;
```

### Numeric Literal Formats

Java supports 4 ways to write integer literals — they are all the same value:

```java
int decimal = 255;          // base 10 (everyday)
int hex     = 0xFF;         // base 16 — prefix 0x
int octal   = 0377;         // base 8  — prefix 0
int binary  = 0b11111111;   // base 2  — prefix 0b (Java 7+)

System.out.println(decimal == hex);    // true — all 255
System.out.println(decimal == binary); // true
```

Underscores improve readability (Java 7+):

```java
long creditCard  = 1234_5678_9012_3456L;
int  phonePincode = 400_001;
int  rgb          = 0xFF_A5_00;   // works in hex too
```

### Stack vs Heap — Where Variables Live

```
  Method: calculate(int a, int b)
  ┌─────────────────────────────┐
  │  STACK FRAME                │
  │  a = 10   (int — 4 bytes)   │  ← primitive lives HERE
  │  b = 3    (int — 4 bytes)   │
  │  result = 13 (int — 4 bytes)│
  └─────────────────────────────┘

  String s = "hello";
  ┌─────────────────────────────┐
  │  STACK FRAME                │
  │  s ──────────────────────┐  │  ← only the REFERENCE is on stack
  └──────────────────────────│──┘
                             ▼
  ┌──────────────────────────────────┐
  │  HEAP                            │
  │  String object: "hello"          │  ← the actual object lives HERE
  └──────────────────────────────────┘
```

**Key implication:** When you pass a primitive to a method, Java passes a
**copy**. Changing it inside the method does not affect the caller.
When you pass an object, Java passes a copy of the **reference** — the object
itself is shared.

---

## 3. Reference Types & the String Pool

Every type that is not one of the 8 primitives is a **reference type** (a class
or interface). Variables hold a reference (memory address) to the object on the
heap, not the object itself.

### String is Special: The String Pool

```java
String s1 = "hello";           // goes into the String Pool
String s2 = "hello";           // reuses the same Pool entry
String s3 = new String("hello"); // explicitly creates a NEW heap object
```

```
  HEAP
  ┌──────────────────────────────────────────────┐
  │                                              │
  │  String Pool (cached area)                   │
  │  ┌──────────────────┐                        │
  │  │   "hello"  ◄──── s1                       │
  │  │           ◄──── s2  (same reference!)     │
  │  └──────────────────┘                        │
  │                                              │
  │  Regular Heap                                │
  │  ┌──────────────────┐                        │
  │  │   "hello"  ◄──── s3  (new object!)        │
  │  └──────────────────┘                        │
  └──────────────────────────────────────────────┘
```

```java
System.out.println(s1 == s2);      // true  — same Pool reference
System.out.println(s1 == s3);      // false — different objects
System.out.println(s1.equals(s3)); // true  — same content
```

> **Rule:** Always use `.equals()` to compare String content.
> `==` compares references (memory addresses), not content.
> This is the #1 beginner bug in Java.

---

## 4. Type Casting

### Widening (Implicit) — Safe, No Data Loss

Java automatically converts a smaller type to a larger type.

```
  byte → short → int → long → float → double
                  ↑
               char (special: char can widen to int)
```

```java
int    i = 100_000;
long   l = i;         // int → long: automatic, always safe
double d = l;         // long → double: automatic BUT may lose precision
                      // for very large longs (> 2^53 ≈ 9 quadrillion)
```

### Narrowing (Explicit) — Risky, May Lose Data

You must explicitly cast when going from a larger to a smaller type.
The compiler forces you to acknowledge the risk.

```java
double pi        = 3.99999;
int    truncated = (int) pi;     // explicit cast: drops decimal → 3 (not 4!)
byte   tiny      = (byte) 200;   // 200 > 127 (byte max) → wraps to -56 (!)
```

```
  What happens with (byte) 200:

  200 in binary (int, 32 bits):
  00000000 00000000 00000000 11001000

  Cast to byte (keep only last 8 bits):
                              11001000  = -56 (two's complement interpretation)
```

This silent data corruption is why narrowing requires an explicit cast — the
compiler is asking you to confirm you understand the risk.

### Integer Overflow — Silent Corruption

```java
int max = Integer.MAX_VALUE; // 2,147,483,647
int bad = max + 1;           // → -2,147,483,648 (wraps around silently!)
```

```
  MAX_VALUE binary: 0111 1111 1111 1111 1111 1111 1111 1111
  +1              : 1000 0000 0000 0000 0000 0000 0000 0000
                    ↑ sign bit flips → negative!
```

Use `Math.addExact()` when overflow must be detected:

```java
try {
    int safe = Math.addExact(Integer.MAX_VALUE, 1);
} catch (ArithmeticException e) {
    System.out.println("Overflow detected!"); // thrown instead of silent wrap
}
```

---

## 5. Autoboxing & the Integer Cache Trap

Java has wrapper classes for each primitive:
`byte→Byte`, `short→Short`, `int→Integer`, `long→Long`,
`float→Float`, `double→Double`, `char→Character`, `boolean→Boolean`

### Autoboxing / Unboxing

The compiler automatically converts between primitive and wrapper:

```java
Integer boxed = 42;    // autoboxing: compiler inserts Integer.valueOf(42)
int unboxed   = boxed; // unboxing:   compiler inserts boxed.intValue()
```

### The Integer Cache — A Production Bug Source

`Integer.valueOf()` caches instances for values **-128 to 127**.
Outside that range, it creates a new object every time.

```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b); // true  — same cached instance

Integer c = 128;
Integer d = 128;
System.out.println(c == d); // false — different objects!
System.out.println(c.equals(d)); // true — use this instead
```

```
  Integer Cache (inside JVM):
  ┌───────────────────────────────────────────────────┐
  │  [-128] [-127] ... [0] [1] ... [127]               │  ← cached
  │     ↑                              ↑               │
  │  Integer.valueOf(-128)     Integer.valueOf(127)     │
  │  always returns same object        same object     │
  └───────────────────────────────────────────────────┘

  Integer.valueOf(128) → new Integer(128) every time
  Integer.valueOf(128) → new Integer(128) every time (different object!)
```

### Null Unboxing — Hidden NPE

```java
Integer value = null;
int     raw   = value; // NullPointerException at runtime!
                       // compiler silently inserts value.intValue()
```

This is subtle because the source code looks like a simple assignment.

> **Rule:** Always use `.equals()` for boxed type comparisons, never `==`.

---

## 6. Operators

### Arithmetic

```java
int a = 10, b = 3;

a + b  →  13       // addition
a - b  →  7        // subtraction
a * b  →  30       // multiplication
a / b  →  3        // INTEGER division: truncates toward zero (NOT floor)
a % b  →  1        // remainder (modulo)
```

**Integer division pitfall — truncation vs floor:**

```java
 7 / 2  →  3    // truncates toward zero
-7 / 2  → -3    // NOT -4 — truncates toward zero (in Java, C, and most languages)
```

**Modulo sign follows the dividend:**

```java
 7 % 3  →  1    // positive dividend → positive remainder
-7 % 3  → -1    // negative dividend → negative remainder
 7 % -3 →  1    // sign of divisor does NOT matter
```

**Pre vs Post Increment:**

```java
int x = 5;

int a = ++x;  // PRE-increment:  x becomes 6 FIRST, then a = 6
int b = x++;  // POST-increment: b = 6 FIRST, then x becomes 7

// Result: a=6, b=6, x=7
```

### Logical Operators — Short-Circuit Evaluation

`&&` and `||` **stop evaluating** as soon as the result is determined.

```java
int x = 0;

// Safe — right side is NEVER reached because left is false
boolean r = (x != 0) && (10 / x > 1);   // no ArithmeticException

// Safe — right side is NEVER reached because left is true
boolean r2 = (x == 0) || (10 / x > 1);  // no ArithmeticException
```

```
  && (AND) short-circuit:
  ┌────────────┐   false   ┌─── skip ───┐
  │  Left side │ ─────────► Right side  │  Result: false (immediately)
  └────────────┘           └────────────┘

  ┌────────────┐   true    ┌────────────┐
  │  Left side │ ─────────► Right side  │  Result: depends on right
  └────────────┘           └────────────┘

  || (OR) short-circuit:
  ┌────────────┐   true    ┌─── skip ───┐
  │  Left side │ ─────────► Right side  │  Result: true (immediately)
  └────────────┘           └────────────┘
```

### Bitwise Operators — Flags and Masks

Used for permission flags, protocol encoding, and performance-critical code.

```java
int a = 0b1010;   // 10
int b = 0b1100;   // 12

a & b  →  0b1000  = 8    // AND:  bit set only if BOTH are 1
a | b  →  0b1110  = 14   // OR:   bit set if EITHER is 1
a ^ b  →  0b0110  = 6    // XOR:  bit set if EXACTLY ONE is 1
~a     →  ...11110101    // NOT:  flip all bits
```

**Real-world pattern — permission flags in one int:**

```java
final int READ    = 0b001;   // bit 0
final int WRITE   = 0b010;   // bit 1
final int EXECUTE = 0b100;   // bit 2

int perms = 0;

// Grant permissions
perms |= READ | WRITE;       // set bits: perms = 0b011

// Check a permission
boolean canRead = (perms & READ) != 0;       // true
boolean canExec = (perms & EXECUTE) != 0;    // false

// Revoke a permission
perms &= ~WRITE;             // clear bit 1: perms = 0b001
```

### Shift Operators

```java
1 << 3   →   8     // left shift = multiply by 2³
8 >> 2   →   2     // signed right shift = divide by 2²  (preserves sign bit)
-1 >> 1  →  -1     // sign bit preserved
-1 >>> 1 →  MAX    // unsigned right shift: fills with 0, not sign bit
```

### Ternary Operator

A concise inline if/else that produces a value:

```java
// Syntax: condition ? valueIfTrue : valueIfFalse
String label = score >= 60 ? "Pass" : "Fail";

// Chained ternary — readable if kept to 3 levels max
String grade = score >= 90 ? "A"
             : score >= 80 ? "B"
             : score >= 70 ? "C"
             : "F";
```

### instanceof with Pattern Matching (Java 16+)

```java
// Old style: check then cast — redundant
if (obj instanceof String) {
    String s = (String) obj;   // you already know it's a String — why cast again?
    System.out.println(s.length());
}

// Pattern matching: check, name, and cast in one expression
if (obj instanceof String s) {
    System.out.println(s.length());   // s is already String, no cast needed
}

// With guard condition (Java 21)
if (obj instanceof Integer i && i > 100) {
    System.out.println("Large integer: " + i);
}
```

---

## 7. Operator Precedence

Higher rows bind tighter (like `*` before `+`).

```
  Precedence (high → low)
  ┌──────────────────────────────────────────┐
  │  1. ()  []  .                            │  ← grouping, access
  │  2. ++  --  ~  !  (type)  (unary)        │
  │  3. *   /   %                            │
  │  4. +   -                                │
  │  5. <<  >>  >>>                          │
  │  6. <   >   <=  >=  instanceof           │
  │  7. ==  !=                               │
  │  8. &                                    │
  │  9. ^                                    │
  │  10. |                                   │
  │  11. &&                                  │
  │  12. ||                                  │
  │  13. ?:  (ternary)                       │
  │  14. =   +=  -=  *=  /=  ... (assignment)│
  └──────────────────────────────────────────┘
```

**Common traps:**

```java
2 + 3 * 4      →  14  (not 20 — multiplication first)
true || false && false  →  true  (reads as: true || (false && false))
```

**Rule:** When in doubt, add parentheses. They cost nothing and prevent bugs.

---

## 8. `var` — Local Type Inference

`var` tells the compiler to infer the type from the right-hand side.
It is **not** dynamic typing — the type is fixed at compile time.

```java
var count = 0;          // inferred: int
var name  = "Alice";    // inferred: String
var list  = new ArrayList<String>(); // inferred: ArrayList<String>
```

**When to use `var`:**

```java
// GOOD: type is obvious from the right side
var map = new HashMap<String, List<Integer>>();  // saves typing
var entry = map.entrySet().iterator().next();    // type is clear from context

// BAD: type is hidden
var result = processTransaction(id);  // what type is result? Forces reader to look up method
var x = getValue();                   // too vague
```

**Where `var` does NOT work:**

```java
var x;               // no initializer — can't infer
var x = null;        // null has no type
// method parameters, return types, or class fields
```

---

## 9. BigDecimal — Why `double` Fails for Money

### The Problem

```java
double a = 0.10;
double b = 0.03;
System.out.println(a + b);          // 0.13000000000000001 ← WRONG
System.out.println(a + b == 0.13);  // false               ← DANGEROUS
```

**Why this happens:** `double` uses binary (base-2) floating-point.
Just as `1/3` cannot be written exactly in decimal (0.333...),
`0.1` cannot be written exactly in binary.

```
  0.1 in binary (approximate):
  0.0001100110011001100110011001100110011... (repeating forever)

  Stored as 64-bit double: 0.1000000000000000055511151231257827021181583404541015625
```

### The Solution — BigDecimal

```java
BigDecimal a = new BigDecimal("0.10");   // pass as String — exact
BigDecimal b = new BigDecimal("0.03");   // pass as String — exact
BigDecimal c = a.add(b);

System.out.println(c);           // 0.13 — exact
System.out.println(c.compareTo(new BigDecimal("0.13")) == 0); // true
```

> Always construct BigDecimal from a **String**, never from a `double`.
> `new BigDecimal(0.1)` captures the imprecise double value.
> `new BigDecimal("0.1")` is exact.

### Rounding

```java
BigDecimal price = new BigDecimal("100.00");
BigDecimal gst   = price.multiply(new BigDecimal("0.18"));
// gst = 18.0000 — too many decimal places

BigDecimal rounded = gst.setScale(2, RoundingMode.HALF_UP); // 18.00
```

| RoundingMode | Behaviour |
|---|---|
| `HALF_UP` | 2.345 → 2.35 (standard rounding — use for money) |
| `HALF_DOWN` | 2.345 → 2.34 |
| `FLOOR` | always rounds toward negative infinity |
| `CEILING` | always rounds toward positive infinity |
| `HALF_EVEN` | banker's rounding — rounds to nearest even digit |

---

## 10. Practical Exercise — Expense Calculator

See the source files for the full implementation:

- `TypesAndVariables.java` — every type with deliberate edge cases
- `Operators.java` — every operator category with non-obvious behaviours
- `ExpenseCalculator.java` — real-world scenario tying it all together:
  uses BigDecimal for GST calculation, bitwise flags for expense status,
  and compound operators for budget tracking

**Run the tests:**

```bash
cd module-02-java-basics
mvn test
```

**Run a specific class:**

```bash
mvn compile exec:java -Dexec.mainClass="com.javatraining.basics.ExpenseCalculator"
```

---

## 11. Exercises

These are meant to be done before looking at the solutions.

**1. Overflow prediction**
What is the output of:
```java
byte b = (byte) 200;
System.out.println(b);
```
Explain using binary representation.

**2. Floating-point trap**
What does this print?
```java
for (double d = 0.0; d != 1.0; d += 0.1) {
    System.out.println(d);
}
```
Will it terminate? Why or why not?

**3. BigDecimal constructor difference**
What is the difference between these two?
```java
new BigDecimal(0.1)
new BigDecimal("0.1")
```
Print both and explain.

**4. Short-circuit guard**
Rewrite this so it never throws ArithmeticException, without using try/catch:
```java
int[] arr = {};
System.out.println(arr.length > 0 & arr[0] == 5);
```

**5. Bitmask design**
Design a bitmask permission system for a user role that supports:
`READ`, `WRITE`, `DELETE`, `ADMIN`. Write methods to grant, revoke, and check permissions.

---

## Next

[Module 03 — Control Flow](../module-03-control-flow/)
