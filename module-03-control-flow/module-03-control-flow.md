---
title: "03 — Control Flow"
parent: "Phase 1 — Fundamentals"
nav_order: 3
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-03-control-flow/src){: .btn .btn-outline }

# Module 03 — Control Flow

> **Phase:** Fundamentals | **Build tool:** Maven | **Java:** 21

---

## Table of Contents

1. [What is Control Flow?](#1-what-is-control-flow)
2. [if / else](#2-if--else)
3. [The Dangling Else Trap](#3-the-dangling-else-trap)
4. [switch — Statement vs Expression](#4-switch--statement-vs-expression)
5. [Switch with Patterns (Java 21)](#5-switch-with-patterns-java-21)
6. [The for Loop](#6-the-for-loop)
7. [The Enhanced for-each Loop](#7-the-enhanced-for-each-loop)
8. [The while Loop](#8-the-while-loop)
9. [The do-while Loop](#9-the-do-while-loop)
10. [break and continue](#10-break-and-continue)
11. [Labels — break and continue in Nested Loops](#11-labels--break-and-continue-in-nested-loops)
12. [Choosing the Right Loop](#12-choosing-the-right-loop)
13. [Practical Exercise](#13-practical-exercise)
14. [Exercises](#14-exercises)

---

## 1. What is Control Flow?

By default, Java executes statements top to bottom, one at a time.
**Control flow** structures let you change that — branch based on conditions,
repeat blocks, or skip ahead.

```
  Normal execution (no control flow):
  ┌──────────┐
  │ stmt 1   │
  └────┬─────┘
       │
  ┌────▼─────┐
  │ stmt 2   │
  └────┬─────┘
       │
  ┌────▼─────┐
  │ stmt 3   │
  └──────────┘

  With if/else (branching):
       ┌──────────────┐
       │  condition?  │
       └──┬───────────┘
     true │         │ false
    ┌─────▼────┐  ┌─▼────────┐
    │ branch A │  │ branch B │
    └─────┬────┘  └────┬─────┘
          └─────┬──────┘
           ┌────▼─────┐
           │  resume  │
           └──────────┘

  With for loop (repetition):
  ┌─────────────────────────┐
  │  init: i = 0            │
  └────────────┬────────────┘
               │
         ┌─────▼──────┐
    ┌────┤ i < limit? ├────┐
    │yes └────────────┘ no │
    │                      │
  ┌─▼──────────┐      ┌────▼─────┐
  │  body      │      │  resume  │
  └─────┬──────┘      └──────────┘
        │
  ┌─────▼──────┐
  │  update    │  (i++)
  └─────┬──────┘
        │
        └───────────────► (back to condition check)
```

---

## 2. if / else

### Basic Syntax

```java
if (condition) {
    // runs when condition is true
} else if (anotherCondition) {
    // runs when first is false AND this is true
} else {
    // runs when ALL conditions above are false
}
```

The `condition` must be a **boolean expression** — not an int, not an object.
(Unlike C/C++, Java does not treat 0 as false or non-zero as true.)

```java
int score = 85;

if (score >= 90) {
    System.out.println("Grade: A");
} else if (score >= 80) {
    System.out.println("Grade: B");   // ← this runs
} else if (score >= 70) {
    System.out.println("Grade: C");
} else {
    System.out.println("Grade: F");
}
```

### One-liner (no braces) — Why to Avoid It

Java allows omitting braces for a single statement:

```java
if (x > 0)
    System.out.println("positive");   // legal, but dangerous
```

This looks fine, but consider what happens if you add a second line:

```java
if (x > 0)
    System.out.println("positive");
    System.out.println("non-zero");   // ALWAYS runs — NOT part of the if!
```

The second `println` is NOT inside the if. The lack of braces only covers the
very next statement. **Always use braces — no exceptions.**

### Common Boolean Mistakes

```java
// WRONG: assignment instead of comparison
if (x = 5) { }       // COMPILE ERROR in Java (unlike C) — boolean required

// WRONG: comparing Strings with ==
String s = "hello";
if (s == "hello") { } // may work by accident (String pool) but is wrong
if (s.equals("hello")) { } // CORRECT — always use .equals() for Strings

// WRONG: redundant boolean comparison
boolean flag = isValid();
if (flag == true) { }   // redundant — flag IS the boolean
if (flag) { }           // CORRECT

if (flag == false) { }  // redundant
if (!flag) { }          // CORRECT
```

---

## 3. The Dangling Else Trap

When you have nested `if` without braces, the `else` attaches to the
**nearest** `if`. This is the "dangling else" problem.

```java
int x = 10, y = 5;

// What do you think this prints?
if (x > 0)
    if (y > 10)
        System.out.println("y > 10");
else
    System.out.println("x <= 0");   // ← which 'if' does this else belong to?
```

You might think the `else` belongs to the outer `if (x > 0)`.
It does NOT. Java attaches `else` to the **nearest if**:

```java
// What Java actually executes:
if (x > 0) {
    if (y > 10) {
        System.out.println("y > 10");
    } else {
        System.out.println("x <= 0");   // runs when y <= 10, NOT when x <= 0
    }
}
// With x=10, y=5: prints "x <= 0" — which is wrong and confusing
```

**Rule: Always use braces. The dangling else is a class of real-world bugs.**

---

## 4. switch — Statement vs Expression

`switch` selects a branch based on a value. Java has two forms:
the old **switch statement** and the modern **switch expression**.

### 4.1 Traditional switch Statement (Java 1–12)

```java
int day = 3;

switch (day) {
    case 1:
        System.out.println("Monday");
        break;         // ← REQUIRED to stop falling through
    case 2:
        System.out.println("Tuesday");
        break;
    case 3:
        System.out.println("Wednesday");
        break;
    default:
        System.out.println("Other");
        break;
}
```

**Fall-through behavior** — if you forget `break`, execution continues into the
next case:

```java
int day = 2;
switch (day) {
    case 1:
        System.out.println("Monday");
    case 2:
        System.out.println("Tuesday");   // ← this runs (day == 2)
    case 3:
        System.out.println("Wednesday"); // ← this ALSO runs (fall-through!)
    default:
        System.out.println("Other");     // ← this ALSO runs (fall-through!)
}
// Output: Tuesday, Wednesday, Other  — probably not what you wanted
```

Fall-through is occasionally intentional (grouping cases), but it is a common
source of bugs. The modern switch expression eliminates it entirely.

### 4.2 Switch Expression with Arrow Syntax (Java 14+)

```java
int day = 3;

// Arrow syntax: each case is an expression, no break needed, no fall-through
String name = switch (day) {
    case 1 -> "Monday";
    case 2 -> "Tuesday";
    case 3 -> "Wednesday";
    case 4 -> "Thursday";
    case 5 -> "Friday";
    case 6 -> "Saturday";
    case 7 -> "Sunday";
    default -> throw new IllegalArgumentException("Invalid day: " + day);
};

System.out.println(name); // Wednesday
```

Key differences from the old switch:
- No `break` — each arrow case is independent, no fall-through
- It's an **expression** — produces a value that can be assigned
- `default` is required when the compiler cannot verify all cases are covered
- Can `throw` in a case

### 4.3 Multiple Labels in One Case

```java
String type = switch (day) {
    case 1, 2, 3, 4, 5 -> "Weekday";
    case 6, 7           -> "Weekend";
    default             -> throw new IllegalArgumentException("Invalid: " + day);
};
```

### 4.4 Switch Expression with yield (multi-line cases)

When a case needs multiple statements, use a block `{}` with `yield` to produce
the value:

```java
int month = 4;
int daysInMonth = switch (month) {
    case 1, 3, 5, 7, 8, 10, 12 -> 31;
    case 4, 6, 9, 11            -> 30;
    case 2 -> {
        // Multi-statement block: compute value and yield it
        boolean leapYear = (2024 % 4 == 0 && 2024 % 100 != 0) || (2024 % 400 == 0);
        yield leapYear ? 29 : 28;   // yield = "return value from this switch block"
    }
    default -> throw new IllegalArgumentException("Invalid month: " + month);
};

System.out.println("Days in April: " + daysInMonth); // 30
```

### 4.5 switch on Strings and Enums

`switch` works on: `byte`, `short`, `int`, `char`, their wrappers, `String`,
and `enum` — not `long`, `float`, `double`, or arbitrary objects.

```java
String command = "start";

String result = switch (command) {
    case "start"  -> "Starting the process...";
    case "stop"   -> "Stopping the process...";
    case "status" -> "Process is running.";
    default       -> "Unknown command: " + command;
};
```

```
  switch vs if-else — when to use which:

  ┌─────────────────────────────────────────────────────────┐
  │  Use switch when:                                       │
  │  - Testing ONE variable against multiple exact values   │
  │  - 3+ branches on the same variable                     │
  │  - Working with String, int, or enum values             │
  │                                                         │
  │  Use if/else when:                                      │
  │  - Conditions are ranges (x > 100, x < 50)             │
  │  - Conditions involve multiple variables                │
  │  - Complex boolean expressions (&&, ||)                 │
  └─────────────────────────────────────────────────────────┘
```

---

## 5. Switch with Patterns (Java 21)

Java 21 adds **pattern matching** in switch — you can match by type and
extract the variable in one step. This is most powerful with sealed classes.

```java
// A sealed hierarchy — all possible subtypes are known at compile time
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius)            implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height)   implements Shape {}

static double area(Shape shape) {
    return switch (shape) {
        case Circle    c  -> Math.PI * c.radius() * c.radius();
        case Rectangle r  -> r.width() * r.height();
        case Triangle  t  -> 0.5 * t.base() * t.height();
        // No default needed — compiler knows all subtypes via sealed
    };
}
```

**With guard conditions** (refining a pattern further):

```java
static String classify(Object obj) {
    return switch (obj) {
        case Integer i when i < 0    -> "negative integer";
        case Integer i when i == 0   -> "zero";
        case Integer i               -> "positive integer: " + i;
        case String  s when s.isEmpty() -> "empty string";
        case String  s               -> "string: " + s;
        case null                    -> "null";
        default                      -> "something else";
    };
}
```

---

## 6. The for Loop

The `for` loop is used when you know **how many times** to iterate.

### Structure

```
  for (initializer ; condition ; update) {
       ─────┬─────   ────┬────   ──┬───
            │            │         │
            │            │         └── runs AFTER each iteration
            │            └──────────── checked BEFORE each iteration
            └───────────────────────── runs ONCE before the loop starts
  }
```

```java
for (int i = 0; i < 5; i++) {
    System.out.println("i = " + i);
}
// Output: 0, 1, 2, 3, 4
// When i reaches 5, condition (i < 5) is false → loop ends
```

### Execution Order — Exactly

```
  Step 1: int i = 0         (initializer — once only)
  Step 2: i < 5 ?  → true   (condition check)
  Step 3: body executes
  Step 4: i++               (update)
  Step 5: i < 5 ?  → true   (condition check again)
  ...
  Step N: i < 5 ?  → false  (loop ends, execution continues after the brace)
```

### Variations

```java
// Counting backwards
for (int i = 10; i >= 0; i--) {
    System.out.print(i + " ");
}
// Output: 10 9 8 7 6 5 4 3 2 1 0

// Step by 2
for (int i = 0; i <= 20; i += 2) {
    System.out.print(i + " ");
}
// Output: 0 2 4 6 8 10 12 14 16 18 20

// Multiple variables (uncommon, but legal)
for (int i = 0, j = 10; i < j; i++, j--) {
    System.out.println("i=" + i + " j=" + j);
}

// Infinite loop (must have a break inside)
for (;;) {
    // body
    if (condition) break;
}
```

### Scope of Loop Variable

```java
for (int i = 0; i < 5; i++) {
    System.out.println(i);
}
// System.out.println(i); // COMPILE ERROR — i is out of scope here
```

---

## 7. The Enhanced for-each Loop

The for-each loop iterates over **arrays** and anything that implements
`Iterable` (all Collections). It is cleaner but less flexible.

```java
int[] numbers = {10, 20, 30, 40, 50};

// Enhanced for-each — no index variable
for (int n : numbers) {
    System.out.println(n);
}
```

```java
List<String> names = List.of("Alice", "Bob", "Charlie");

for (String name : names) {
    System.out.println(name.toUpperCase());
}
```

### Limitations of for-each

```java
int[] arr = {1, 2, 3, 4, 5};

// CANNOT modify elements (n is a copy)
for (int n : arr) {
    n = n * 2;    // modifies the copy, NOT the array
}
// arr is unchanged — use a regular for loop to modify elements

// CANNOT access the index
for (int n : arr) {
    // no way to know which position n came from
    // use a regular for loop if you need the index
}

// CANNOT iterate two collections in parallel
// use a regular for loop with an index for that
```

```
  for-each vs for — decision:

  ┌─────────────────────────────────────────────────────┐
  │  Use for-each when:                                 │
  │  - Just reading/processing each element             │
  │  - Don't need the index                             │
  │  - Not modifying the collection during iteration    │
  │                                                     │
  │  Use regular for when:                              │
  │  - Need the index                                   │
  │  - Modifying elements by position                   │
  │  - Iterating in reverse                             │
  │  - Skipping elements (step > 1)                     │
  │  - Two collections in sync                          │
  └─────────────────────────────────────────────────────┘
```

---

## 8. The while Loop

The `while` loop is used when you **don't know in advance** how many iterations
are needed — you loop until a condition becomes false.

```
  while (condition) {
      body
  }

  Flow:
  ┌──────────────┐
  │  condition?  │ ◄─────────────┐
  └──┬───────────┘               │
     │ true                      │
  ┌──▼──────────┐                │
  │    body     │────────────────┘
  └─────────────┘
     │ false
  ┌──▼──────────┐
  │   resume    │
  └─────────────┘
```

```java
// Keep asking for a positive number
Scanner scanner = new Scanner(System.in);
int input = -1;

while (input <= 0) {
    System.out.print("Enter a positive number: ");
    input = scanner.nextInt();
}
System.out.println("You entered: " + input);
```

```java
// Digit extraction — number of iterations unknown until runtime
int number = 12345;
while (number > 0) {
    int digit = number % 10;          // extract last digit
    System.out.println(digit);        // 5, 4, 3, 2, 1
    number = number / 10;             // remove last digit
}
```

**Infinite while loop** — common and idiomatic for servers and event loops:

```java
while (true) {
    // process next event/request
    if (shouldStop()) break;
}
```

---

## 9. The do-while Loop

`do-while` is like `while`, except the body executes **at least once**
before the condition is checked.

```
  do {
      body        ← always runs at least once
  } while (condition);

  Flow:
  ┌─────────────┐
  │    body     │ ◄─────────────┐
  └──────┬──────┘               │
         │                      │ true
  ┌──────▼───────┐              │
  │  condition?  │──────────────┘
  └──────┬───────┘
         │ false
  ┌──────▼───────┐
  │    resume    │
  └──────────────┘
```

```java
// Menu loop — must show the menu at least once before checking the choice
int choice;
do {
    System.out.println("1. Add  2. Remove  3. View  0. Exit");
    System.out.print("Choice: ");
    choice = scanner.nextInt();
    processChoice(choice);
} while (choice != 0);
```

```
  while vs do-while:

  ┌─────────────────────────────────────────────────────┐
  │  while:    condition checked BEFORE first iteration  │
  │            body may never run (if condition starts  │
  │            false)                                   │
  │                                                     │
  │  do-while: body always runs AT LEAST ONCE           │
  │            condition checked AFTER first iteration  │
  │                                                     │
  │  Use do-while for: menus, "retry" loops, prompting  │
  │  the user at least once before validating input     │
  └─────────────────────────────────────────────────────┘
```

---

## 10. break and continue

### break — Exit the Loop Immediately

```java
// Find the first negative number and stop
int[] data = {5, 8, 3, -2, 7, 1};
int firstNegative = -1;

for (int n : data) {
    if (n < 0) {
        firstNegative = n;
        break;          // stop searching — no point continuing
    }
}
System.out.println("First negative: " + firstNegative); // -2
```

`break` only exits the **innermost** loop. In nested loops, it exits just
the loop it's directly inside.

### continue — Skip This Iteration, Go to Next

```java
// Print only even numbers
for (int i = 0; i < 10; i++) {
    if (i % 2 != 0) {
        continue;       // skip odd numbers — jump to i++
    }
    System.out.print(i + " ");
}
// Output: 0 2 4 6 8
```

```
  continue in a for loop:
  ┌──────────────────────────┐
  │  condition check (i < 10)│ ◄──────────────────────┐
  └──────────┬───────────────┘                         │
             │ true                                     │
  ┌──────────▼──────────┐                              │
  │  i % 2 != 0 ?       │                              │
  └──┬──────────────┬───┘                              │
     │ yes          │ no                                │
     │         ┌────▼────────────┐                     │
     │         │  println(i)     │                     │
     │         └────┬────────────┘                     │
     │              │                                   │
     └──────────────┘                                   │
                    │                                   │
             ┌──────▼──────┐                           │
             │   i++       │───────────────────────────┘
             └─────────────┘
  (continue jumps directly to the update step i++)
```

---

## 11. Labels — break and continue in Nested Loops

Plain `break` and `continue` only affect the **innermost** loop. Labels let
you target an **outer** loop.

### Problem without Labels

```java
// Find the first pair (i, j) where i * j > 20
outer:
for (int i = 1; i <= 5; i++) {
    for (int j = 1; j <= 5; j++) {
        if (i * j > 20) {
            System.out.println("Found: i=" + i + " j=" + j);
            break;        // only breaks inner loop — outer keeps running!
        }
    }
}
// Keeps running for all values of i — not what we wanted
```

### Solution with a Label

```java
// Label marks the outer loop
search:
for (int i = 1; i <= 5; i++) {
    for (int j = 1; j <= 5; j++) {
        if (i * j > 20) {
            System.out.println("Found: i=" + i + " j=" + j);
            break search;   // breaks OUT of the loop labeled 'search' (the outer one)
        }
    }
}
System.out.println("Done");
```

```java
// continue with a label: skip to the NEXT ITERATION of the outer loop
grid:
for (int row = 0; row < 3; row++) {
    for (int col = 0; col < 3; col++) {
        if (col == 1) {
            continue grid;  // skip to next row entirely (not just next col)
        }
        System.out.println("row=" + row + " col=" + col);
    }
}
// Only col=0 is printed for each row — col=1 and col=2 are never reached
```

```
  Label diagram:

  outerLoop:          ← label on outer for
  for (...) {
      innerLoop:      ← label on inner for
      for (...) {
          break outerLoop;   → jumps OUT of outerLoop entirely
          break innerLoop;   → same as plain break (exits innerLoop)
          continue outerLoop → jumps to next iteration of outerLoop
          continue innerLoop → same as plain continue (next iter of innerLoop)
      }
  }
```

> **Note:** Labels are rarely used in production code. If you find yourself
> needing them often, it's usually a sign that the code should be extracted
> into a method with a `return` instead.

---

## 12. Choosing the Right Loop

```
  ┌─────────────────────────────────────────────────────────────┐
  │  Known number of iterations?                                │
  │  → for loop                                                 │
  │    for (int i = 0; i < n; i++)                              │
  │                                                             │
  │  Iterating over a collection/array (no index needed)?       │
  │  → for-each                                                 │
  │    for (Item item : collection)                             │
  │                                                             │
  │  Unknown iterations, check condition BEFORE body?           │
  │  → while                                                    │
  │    while (condition) { ... }                                │
  │                                                             │
  │  Must execute body at least once (e.g., menu, retry)?       │
  │  → do-while                                                 │
  │    do { ... } while (condition);                            │
  │                                                             │
  │  Event loop / server / "run forever until signal"?          │
  │  → while (true) { ... if (stop) break; }                   │
  └─────────────────────────────────────────────────────────────┘
```

---

## 13. Practical Exercise

### Files in this module

| File | What it demonstrates |
|------|----------------------|
| `ConditionalDemo.java` | if/else, dangling else, boolean mistakes, ternary |
| `SwitchDemo.java` | Traditional switch, switch expression, yield, pattern matching |
| `LoopDemo.java` | All loop types, break, continue, labels |
| `NumberAnalyzer.java` | Practical exercise — ties all control flow together |

### NumberAnalyzer — What it Does

A command-line number analysis tool that:
- Uses `do-while` to keep the program running until the user quits
- Uses `switch` expression to choose the analysis mode
- Uses `for` loops to process ranges
- Uses `while` to find values matching a condition
- Uses `break`/`continue`/labels for early termination

**Run it:**

```bash
cd module-03-control-flow
mvn compile exec:java -Dexec.mainClass="com.javatraining.controlflow.NumberAnalyzer"
```

**Run the tests:**

```bash
mvn test
```

---

## 14. Exercises

**1. FizzBuzz (classic)**
Print numbers 1–100. For multiples of 3 print "Fizz", multiples of 5 print
"Buzz", multiples of both print "FizzBuzz". Use a `switch` expression, not
`if/else`.

**2. Prime Finder**
Write a method `boolean isPrime(int n)` using a `for` loop and `break`.
Then find all primes up to 100 and print them.

**3. Pyramid Pattern**
Print this pattern for `n = 5` using nested `for` loops:
```
*
* *
* * *
* * * *
* * * * *
```

**4. Digit Sum**
Given any integer (including negatives), compute the sum of its digits using
a `while` loop. `digitSum(1234) = 10`, `digitSum(-987) = 24`.

**5. Menu System**
Build a `do-while` + `switch` expression menu that offers:
- Option 1: check if a number is prime
- Option 2: compute factorial
- Option 3: reverse a number's digits
- Option 0: exit
It must not crash on invalid input.

---

## Next

[Module 04 — Methods](../module-04-methods/)
