---
title: "05 — Arrays & Strings"
parent: "Phase 1 — Fundamentals"
nav_order: 5
render_with_liquid: false
---

# Module 05 — Arrays, Strings & Regex

> **Phase:** Fundamentals | **Build tool:** Maven | **Java:** 21

---

## Table of Contents

1. [Arrays](#1-arrays)
2. [Multi-Dimensional & Jagged Arrays](#2-multi-dimensional--jagged-arrays)
3. [The Arrays Utility Class](#3-the-arrays-utility-class)
4. [String — Immutability & the String Pool](#4-string--immutability--the-string-pool)
5. [String API — Every Method You Actually Use](#5-string-api--every-method-you-actually-use)
6. [String Concatenation & Performance](#6-string-concatenation--performance)
7. [StringBuilder & StringJoiner](#7-stringbuilder--stringjoiner)
8. [Text Blocks (Java 13+)](#8-text-blocks-java-13)
9. [Regular Expressions](#9-regular-expressions)
10. [Regex Groups & Named Groups](#10-regex-groups--named-groups)
11. [Practical Exercise — CSV Text Processor](#11-practical-exercise--csv-text-processor)
12. [Exercises](#12-exercises)

---

## 1. Arrays

An **array** is a fixed-size, ordered container that holds elements of a
single type. All elements are stored in contiguous memory — this makes
index access O(1) but resizing impossible (you must create a new array).

### Declaration and Initialization

```java
// Declaration — no memory allocated yet
int[] numbers;

// Allocation — memory allocated, elements set to default values
numbers = new int[5];        // [0, 0, 0, 0, 0]

// Combined declaration + allocation
int[] scores = new int[3];   // [0, 0, 0]

// Array literal — declare, allocate, and initialize at once
int[] primes = {2, 3, 5, 7, 11};

// new keyword form of literal (required when passing inline to a method)
printAll(new int[]{10, 20, 30});
```

### Default Values

Every newly created array is pre-filled with the type's default value:

| Element type | Default value |
|---|---|
| `int`, `long`, `short`, `byte` | `0` |
| `double`, `float` | `0.0` |
| `boolean` | `false` |
| `char` | `'\u0000'` (null char) |
| Any object / String | `null` |

### Memory Layout

```
  int[] arr = {10, 20, 30, 40, 50};

  STACK                 HEAP
  ┌──────────┐          ┌────┬────┬────┬────┬────┐
  │ arr ─────┼─────────►│ 10 │ 20 │ 30 │ 40 │ 50 │
  └──────────┘          └────┴────┴────┴────┴────┘
                         [0]  [1]  [2]  [3]  [4]
                                   ▲
                             arr[2] = 30 (O(1) access by index)
```

### Important Properties

```java
int[] data = {10, 20, 30, 40, 50};

// Length — number of elements (NOT last index)
System.out.println(data.length);    // 5

// Last valid index is always length - 1
System.out.println(data[data.length - 1]);  // 50

// ArrayIndexOutOfBoundsException — accessing out-of-range index
// data[5];   ← throws at runtime — no compile-time check
// data[-1];  ← throws at runtime

// Arrays are objects — they have identity
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
System.out.println(a == b);              // false — different objects
System.out.println(Arrays.equals(a, b)); // true  — same content
```

### Copying Arrays

```java
int[] src = {1, 2, 3, 4, 5};

// System.arraycopy — fastest, most flexible (src, srcPos, dest, destPos, length)
int[] dest1 = new int[5];
System.arraycopy(src, 0, dest1, 0, src.length);  // copy all

// Arrays.copyOf — creates a new array of specified length
int[] dest2 = Arrays.copyOf(src, 3);       // [1, 2, 3]    (truncates)
int[] dest3 = Arrays.copyOf(src, 8);       // [1,2,3,4,5,0,0,0] (pads with 0)

// Arrays.copyOfRange — copy a slice
int[] dest4 = Arrays.copyOfRange(src, 1, 4); // [2, 3, 4]  (index 1 inclusive to 4 exclusive)

// clone() — shallow copy of the whole array
int[] dest5 = src.clone();                   // [1, 2, 3, 4, 5]
```

> **Shallow vs Deep copy:** For primitive arrays, all copies are deep (no
> shared state). For object arrays, all copies are shallow — both arrays point
> to the **same** objects. Modifying an object through one array IS visible via
> the other.

---

## 2. Multi-Dimensional & Jagged Arrays

### 2D Arrays (Matrix)

```java
// Declaration: int[rows][cols]
int[][] matrix = new int[3][4];    // 3 rows, 4 cols — all 0

// Literal initialization
int[][] grid = {
    {1, 2, 3},
    {4, 5, 6},
    {7, 8, 9}
};

// Access: [row][col]
System.out.println(grid[1][2]);    // 6 (row 1, col 2)

// Iterate with nested for loops
for (int row = 0; row < grid.length; row++) {
    for (int col = 0; col < grid[row].length; col++) {
        System.out.printf("%3d", grid[row][col]);
    }
    System.out.println();
}
```

```
  grid memory layout:

  STACK          HEAP (outer array — array of references)
  ┌───────┐      ┌──────┬──────┬──────┐
  │grid ──┼─────►│ ref0 │ ref1 │ ref2 │
  └───────┘      └──┬───┴──┬───┴──┬───┘
                    │      │      │
                  ┌─▼──┐ ┌─▼──┐ ┌─▼──┐   (inner arrays on heap)
                  │1,2,3│ │4,5,6│ │7,8,9│
                  └────┘ └────┘ └────┘
```

### Jagged Arrays (Rows of Different Lengths)

```java
// Jagged: each row can have a different length
int[][] triangle = new int[5][];   // outer array of 5 rows, inner not yet allocated

for (int row = 0; row < triangle.length; row++) {
    triangle[row] = new int[row + 1];    // row 0 has 1 element, row 4 has 5
    Arrays.fill(triangle[row], row + 1);
}
// triangle[0] = [1]
// triangle[1] = [2, 2]
// triangle[2] = [3, 3, 3]
// ...
```

---

## 3. The Arrays Utility Class

`java.util.Arrays` contains static helpers for every common array operation.

```java
int[] data = {5, 2, 8, 1, 9, 3, 7, 4, 6};

// --- Sorting ---
Arrays.sort(data);                        // in-place sort: [1,2,3,4,5,6,7,8,9]

// Sort a range [fromIndex, toIndex)
int[] partial = {5, 2, 8, 1, 9, 3};
Arrays.sort(partial, 1, 4);               // sort indices 1,2,3 only: [5,1,2,8,9,3]

// Sort objects with a comparator
String[] words = {"banana", "apple", "cherry", "date"};
Arrays.sort(words);                       // natural order: [apple, banana, cherry, date]
Arrays.sort(words, Comparator.comparingInt(String::length)); // by length

// --- Searching (array MUST be sorted first) ---
int[] sorted = {1, 2, 3, 4, 5, 6, 7, 8, 9};
int idx = Arrays.binarySearch(sorted, 6); // returns 5 (index)
int missing = Arrays.binarySearch(sorted, 10); // returns negative (not found)

// --- Filling ---
int[] filled = new int[5];
Arrays.fill(filled, 99);                  // [99, 99, 99, 99, 99]
Arrays.fill(filled, 1, 4, 0);            // set indices 1-3 to 0: [99,0,0,0,99]

// --- Equality ---
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
System.out.println(Arrays.equals(a, b));         // true (1D comparison)

int[][] m1 = {{1,2},{3,4}};
int[][] m2 = {{1,2},{3,4}};
System.out.println(Arrays.equals(m1, m2));       // false! (shallow — compares refs)
System.out.println(Arrays.deepEquals(m1, m2));   // true   (recursive content check)

// --- Printing ---
System.out.println(Arrays.toString(a));           // [1, 2, 3]
System.out.println(Arrays.deepToString(m1));      // [[1, 2], [3, 4]]

// --- Stream / List conversion ---
int[]    nums  = {3, 1, 4, 1, 5};
int[]    copy  = Arrays.stream(nums).sorted().toArray(); // [1,1,3,4,5]
String[] words2 = {"a", "b", "c"};
List<String> list = Arrays.asList(words2);        // fixed-size List backed by the array
```

> **`Arrays.asList()` trap:** The returned List is **fixed-size** — you can
> call `set()` but NOT `add()` or `remove()`. It throws `UnsupportedOperationException`.
> To get a mutable list: `new ArrayList<>(Arrays.asList(arr))`

---

## 4. String — Immutability & the String Pool

### String is Immutable

Once created, a `String` object's content **never changes**. Every "modifying"
operation returns a **new** String object.

```java
String s = "hello";
s.toUpperCase();                  // creates a new String "HELLO" — s is unchanged
System.out.println(s);            // still "hello"

s = s.toUpperCase();              // now s points to the new String
System.out.println(s);            // "HELLO"
```

```
  HEAP before:            HEAP after s = s.toUpperCase():
  ┌──────────┐            ┌──────────┐   ┌──────────┐
  │ "hello"  │◄── s       │ "hello"  │   │ "HELLO"  │◄── s
  └──────────┘            └──────────┘   └──────────┘
                           (orphaned,      (new object)
                            GC-eligible)
```

**Why immutability?**
- **Thread safety** — multiple threads can read the same String with no locks
- **String Pool** — safe to share because nobody can change the content
- **Security** — file paths, network addresses, credentials can't be altered
  after validation
- **HashMap keys** — hashCode never changes, safe to use as key

### The String Pool

String literals are stored in a special region of heap called the **String Pool**
(also called interned strings). Two literals with the same content share one object.

```java
String s1 = "java";           // stored in pool
String s2 = "java";           // reuses the SAME pool entry
String s3 = new String("java"); // FORCES a new heap object, bypasses pool
String s4 = s3.intern();       // manually adds s3's value to pool, returns pool ref

System.out.println(s1 == s2);  // true  — same pool reference
System.out.println(s1 == s3);  // false — s3 is not in pool
System.out.println(s1 == s4);  // true  — s4 was interned

// Compile-time constant folding — compiler combines literal expressions
String s5 = "ja" + "va";       // compiler sees this as "java" at compile time
System.out.println(s1 == s5);  // true — compiler-folded into pool literal

// Runtime concatenation — NOT folded
String part = "ja";
String s6 = part + "va";       // runtime: creates new heap object
System.out.println(s1 == s6);  // false
```

```
  String Pool (inside heap):
  ┌────────────────────────────────────────┐
  │  "java"  ◄──── s1, s2, s4, s5         │
  │  "hello" ◄──── (other literals)       │
  └────────────────────────────────────────┘

  Regular heap:
  ┌────────────────────────────────────────┐
  │  "java"  ◄──── s3  (new String(...))  │
  └────────────────────────────────────────┘
```

> **Rule:** Always compare String content with `.equals()`, never `==`.

---

## 5. String API — Every Method You Actually Use

### Inspection

```java
String s = "  Hello, World!  ";

s.length()                    // 18 — includes spaces
s.isEmpty()                   // false — true only if length() == 0
s.isBlank()                   // false — true if only whitespace (Java 11+)
s.charAt(7)                   // 'W'
s.indexOf('o')                // 4  — first occurrence, -1 if not found
s.lastIndexOf('o')            // 9  — last occurrence
s.indexOf("World")            // 8  — substring search
s.contains("World")           // true
s.startsWith("  Hello")       // true
s.endsWith("!  ")             // true
```

### Extraction

```java
String s = "Hello, World!";

s.substring(7)                // "World!" — from index 7 to end
s.substring(7, 12)            // "World"  — [7, 12) — 12 is exclusive
s.charAt(0)                   // 'H'
s.toCharArray()               // char[] {'H','e','l','l','o',',', ...}
```

### Transformation (each returns a NEW String)

```java
String s = "  Hello, World!  ";

s.toLowerCase()               // "  hello, world!  "
s.toUpperCase()               // "  HELLO, WORLD!  "
s.trim()                      // "Hello, World!"  — removes ASCII whitespace
s.strip()                     // "Hello, World!"  — Unicode-aware (Java 11+, prefer this)
s.stripLeading()              // "Hello, World!  "
s.stripTrailing()             // "  Hello, World!"
s.replace('l', 'r')           // "  Herro, Worrd!  " — char replacement
s.replace("World", "Java")    // "  Hello, Java!  " — literal string replacement
s.replaceAll("\\s+", "-")     // regex replacement — all whitespace runs → "-"
s.replaceFirst("\\s", "_")    // replaces FIRST whitespace match only
```

### Splitting & Joining

```java
// split — returns String[], regex-based
"a,b,c,d".split(",")          // ["a", "b", "c", "d"]
"a,,b".split(",")             // ["a", "", "b"]
"a,,b".split(",", -1)         // ["a", "", "b"]   (limit=-1 keeps trailing empties)
"a,,b,,".split(",")           // ["a", "", "b"]   (trailing empties dropped by default)
"one two  three".split("\\s+")// ["one", "two", "three"] — one or more spaces

// join — static method
String.join(", ", "Alice", "Bob", "Charlie") // "Alice, Bob, Charlie"
String.join("-", List.of("2024", "04", "15")) // "2024-04-15"
```

### Comparison

```java
String a = "Hello";
String b = "hello";

a.equals(b)                   // false — case-sensitive
a.equalsIgnoreCase(b)         // true
a.compareTo(b)                // negative — 'H' < 'h' in Unicode
a.compareToIgnoreCase(b)      // 0 — equal ignoring case
```

### Conversion

```java
// Primitive → String
String.valueOf(42)            // "42"
String.valueOf(3.14)          // "3.14"
String.valueOf(true)          // "true"
Integer.toString(255, 16)     // "ff" — base-16 representation

// String → primitive
Integer.parseInt("42")        // 42
Double.parseDouble("3.14")    // 3.14
Boolean.parseBoolean("true")  // true
// Note: throws NumberFormatException if the string is not a valid number

// Formatting
String.format("%-10s %5d", "item", 42)  // "item        42"
```

### Useful Java 11–21 String Methods

```java
"  \t\n  ".isBlank()          // true  — whitespace-only check
"line1\nline2\nline3"
    .lines()                  // Stream<String> — split by line terminators
    .collect(...)

"abc".repeat(3)               // "abcabcabc" (Java 11+)

"  hello  ".stripLeading()    // "hello  "
"  hello  ".stripTrailing()   // "  hello"

// Indentation and normalization (Java 12+)
"hello\nworld".indent(4)      // "    hello\n    world\n"
```

---

## 6. String Concatenation & Performance

### The + Operator in Loops — Hidden Performance Trap

```java
// WRONG — creates a new String object on EVERY iteration
String result = "";
for (int i = 0; i < 10_000; i++) {
    result += i;    // internally: result = new StringBuilder(result).append(i).toString()
}
// Creates 10,000 intermediate String objects → heavy GC pressure
```

```
  Iteration 1: result = "" + 0   → new String "0"       (old "" eligible for GC)
  Iteration 2: result = "0" + 1  → new String "01"      (old "0" eligible for GC)
  Iteration 3: result = "01" + 2 → new String "012"     ...
  ...10,000 objects created and thrown away
```

```java
// CORRECT — one StringBuilder, append is amortized O(1)
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10_000; i++) {
    sb.append(i);
}
String result = sb.toString();  // one final String created
```

### When + is Fine

The Java compiler automatically converts compile-time `+` into `StringBuilder`
when it can. You only need to manually use `StringBuilder` in loops or when
building strings conditionally across many statements.

```java
// Fine — compiler optimizes this to a single StringBuilder chain
String name = "Alice";
int age = 30;
String msg = "Name: " + name + ", Age: " + age;
```

---

## 7. StringBuilder & StringJoiner

### StringBuilder — Mutable String Buffer

```java
StringBuilder sb = new StringBuilder("Hello");

sb.append(", ")                 // "Hello, "
  .append("World")             // "Hello, World"
  .append('!');                // "Hello, World!"  (chaining — each append returns 'this')

sb.insert(5, " Beautiful");    // "Hello Beautiful, World!"
sb.delete(5, 15);              // "Hello, World!"   (delete [5,15))
sb.deleteCharAt(12);           // "Hello, World"
sb.replace(7, 12, "Java");     // "Hello, Java"
sb.reverse();                  // "avaJ ,olleH"

sb.length()                    // current character count
sb.charAt(0)                   // 'a'
sb.indexOf("J")                // 4
sb.toString()                  // produce the final immutable String

// Capacity management — StringBuilder pre-allocates internal buffer
new StringBuilder()            // default capacity: 16 chars
new StringBuilder(256)         // pre-allocate 256 — avoids resizing in tight loops
```

### StringJoiner — Joining with a Delimiter

```java
// Joining with separator, optional prefix and suffix
StringJoiner sj = new StringJoiner(", ", "[", "]");
sj.add("Alice");
sj.add("Bob");
sj.add("Carol");
System.out.println(sj);        // [Alice, Bob, Carol]

// Handles empty case gracefully
StringJoiner empty = new StringJoiner(", ", "[", "]");
empty.setEmptyValue("(none)");
System.out.println(empty);     // (none)

// String.join is shorthand for StringJoiner without prefix/suffix
String.join(", ", "a", "b", "c")  // "a, b, c"
String.join(" | ", list)           // joins any Iterable
```

---

## 8. Text Blocks (Java 13+)

Text blocks let you write multi-line strings without escape sequences.

```java
// Old way — unreadable, error-prone
String json = "{\n" +
              "  \"name\": \"Alice\",\n" +
              "  \"age\": 30\n" +
              "}";

// Text block — what you see is what you get
String json = """
        {
          "name": "Alice",
          "age": 30
        }
        """;
// The closing """ determines the indentation baseline.
// All content is dedented by the number of leading spaces on the closing """.

// HTML, SQL, and JSON become very readable:
String sql = """
        SELECT u.name, u.email
        FROM   users u
        JOIN   orders o ON u.id = o.user_id
        WHERE  o.status = 'PENDING'
        ORDER  BY o.created_at DESC
        LIMIT  100
        """;
```

```
  Indentation rules:
  ┌──────────────────────────────────────────────────────────────┐
  │  The closing """ sets the baseline.                          │
  │  Java strips that many leading spaces from every line.       │
  │                                                              │
  │  String s = """                                              │
  │          hello      ← 10 spaces before 'h'                  │
  │          world      ← 10 spaces before 'w'                  │
  │          """;       ← 10 spaces before """ → baseline = 10  │
  │                                                              │
  │  Result: "hello\nworld\n"  (10 spaces stripped from each)   │
  └──────────────────────────────────────────────────────────────┘
```

---

## 9. Regular Expressions

A **regular expression** (regex) is a pattern used to match, search, or
replace text. Java uses `java.util.regex.Pattern` and `Matcher`.

### Core Syntax

```
  Character classes:
  .        any character except newline
  \d       digit [0-9]
  \D       non-digit
  \w       word character [a-zA-Z0-9_]
  \W       non-word character
  \s       whitespace (space, tab, newline)
  \S       non-whitespace
  [abc]    one of: a, b, or c
  [^abc]   NOT one of: a, b, or c
  [a-z]    range: a through z

  Quantifiers:
  *        0 or more
  +        1 or more
  ?        0 or 1 (optional)
  {n}      exactly n times
  {n,}     n or more times
  {n,m}    between n and m times (inclusive)
  *?  +?   lazy (match as few as possible — default is greedy)

  Anchors:
  ^        start of string (or line in MULTILINE mode)
  $        end of string (or line in MULTILINE mode)
  \b       word boundary
  \B       non-word boundary

  Groups:
  (abc)    capturing group — can be referenced as \1, \2, etc.
  (?:abc)  non-capturing group — groups without capturing
  (?=abc)  positive lookahead — matches position followed by "abc"
  (?!abc)  negative lookahead
```

### Pattern and Matcher

```java
import java.util.regex.*;

// Compile once, reuse many times — Pattern is immutable and thread-safe
Pattern emailPattern = Pattern.compile(
    "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
);

// Matcher is stateful — create per-use
Matcher m = emailPattern.matcher("user@example.com");
System.out.println(m.matches());   // true — matches() checks the ENTIRE string

// find() — searches for pattern anywhere in the string (partial match)
Pattern digits = Pattern.compile("\\d+");
Matcher finder = digits.matcher("Order 12345 placed on 2024-04-15");
while (finder.find()) {
    System.out.println("Found: " + finder.group()
        + " at [" + finder.start() + ", " + finder.end() + ")");
}
// Found: 12345 at [6, 11)
// Found: 2024  at [22, 26)
// Found: 04    at [27, 29)
// Found: 15    at [30, 32)
```

### matches() vs find() vs lookingAt()

```
  String:  "  Hello World  "
  Pattern: "Hello"

  ┌─────────────────┬───────────────────────────────────────────────┐
  │  matches()      │ false — requires ENTIRE string to match       │
  │  find()         │ true  — finds "Hello" anywhere in the string  │
  │  lookingAt()    │ false — must match from the START (not end)   │
  └─────────────────┴───────────────────────────────────────────────┘
```

### Quick String Methods with Regex

```java
// String.matches() — shorthand for full-string match
"12345".matches("\\d+")              // true
"12a45".matches("\\d+")             // false

// replaceAll / replaceFirst
"hello   world".replaceAll("\\s+", " ")   // "hello world"
"2024-04-15".replaceAll("-", "/")         // "2024/04/15"

// split with regex
"one, two,  three".split(",\\s*")    // ["one", "two", "three"]
```

---

## 10. Regex Groups & Named Groups

Groups let you **capture** parts of the matched string for extraction.

```java
// Capturing group: (...)
Pattern datePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
Matcher m = datePattern.matcher("Order placed on 2024-04-15 at 10:30");

if (m.find()) {
    System.out.println("Full match: " + m.group(0));  // "2024-04-15"
    System.out.println("Year:  "      + m.group(1));  // "2024"
    System.out.println("Month: "      + m.group(2));  // "04"
    System.out.println("Day:   "      + m.group(3));  // "15"
}
```

### Named Groups — Self-Documenting Patterns

```java
// Named group syntax: (?<name>pattern)
Pattern namedDate = Pattern.compile(
    "(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})"
);

Matcher m = namedDate.matcher("2024-04-15");
if (m.matches()) {
    System.out.println("Year:  " + m.group("year"));   // "2024"
    System.out.println("Month: " + m.group("month"));  // "04"
    System.out.println("Day:   " + m.group("day"));    // "15"
}
```

### Back-References in Replacement

```java
// Use $1, $2, ... in replacement strings to refer to captured groups
"2024-04-15".replaceAll(
    "(\\d{4})-(\\d{2})-(\\d{2})",
    "$3/$2/$1"
)
// "15/04/2024" — rearranged using back-references
```

### Common Production Patterns

```java
// Email (simplified but practical)
"^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"

// Indian mobile number
"^[6-9]\\d{9}$"

// Integer (with optional sign)
"^-?\\d+$"

// Decimal number
"^-?\\d+(\\.\\d+)?$"

// Date YYYY-MM-DD
"^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$"

// URL (basic)
"^https?://[\\w\\-]+(\\.[\\w\\-]+)+(/[\\w\\-./?%&=]*)?$"

// Whitespace normalization
"\\s+"     →  replace with " "

// Strip HTML tags
"<[^>]*>"  →  replace with ""
```

> **Compile Pattern objects once.** `Pattern.compile()` is expensive —
> it parses and compiles the regex. Store compiled patterns as
> `static final` fields to avoid recompiling on every call.

---

## 11. Practical Exercise

### Files in this Module

| File | What it demonstrates |
|------|----------------------|
| `ArraysDemo.java` | Arrays, copying, sorting, Arrays utility class, object arrays |
| `StringsDemo.java` | Immutability, String pool, full String API, text blocks |
| `RegexDemo.java` | Pattern/Matcher, groups, named groups, common patterns |
| `TextProcessor.java` | Practical CSV parser + validator combining all three topics |

### TextProcessor — What it Does

Processes a CSV file of user records:
- Uses `split()` and `String` methods to parse each row
- Validates email and phone fields using compiled `Pattern` objects
- Uses `StringBuilder` to build formatted output reports
- Uses `Arrays.sort()` to sort records
- Handles malformed rows with clear error messages

**Run:**
```bash
cd module-05-arrays-strings
mvn compile exec:java -Dexec.mainClass="com.javatraining.arrays.TextProcessor"
```

**Test:**
```bash
mvn test
```

---

## 12. Exercises

**1. Array rotation**
Write `rotate(int[] arr, int k)` that rotates the array left by `k` positions
in-place (without a second array). `rotate([1,2,3,4,5], 2)` → `[3,4,5,1,2]`.

**2. String palindrome**
Write `isPalindrome(String s)` that ignores case and non-alphanumeric characters.
`isPalindrome("A man, a plan, a canal: Panama")` → `true`.

**3. Word frequency**
Given a sentence, return a `Map<String, Integer>` of word → count, case-insensitive,
ignoring punctuation. Use `split()` and regex.

**4. StringBuilder performance**
Measure the time difference between:
- Building a 100,000-character string with `+` in a loop
- Building it with `StringBuilder`
Use `System.nanoTime()` for timing.

**5. Regex extractor**
Write a method that extracts all URLs from a block of HTML text using
`Pattern`/`Matcher`. Return them as a `List<String>`.

---

## Next

[Module 06 — Enums](../module-06-enums/)
