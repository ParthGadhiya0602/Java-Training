package com.javatraining.basics;

/**
 * TOPIC: Primitive types, reference types, literals, type casting, and the
 * difference between stack and heap allocation.
 *
 * KEY INSIGHT: Java has exactly 8 primitive types. Everything else is a class.
 * Primitives live on the stack; objects live on the heap. This distinction
 * drives performance, null safety, and serialization decisions in real code.
 */
public class TypesAndVariables {

    // -------------------------------------------------------------------------
    // PRIMITIVE TYPES
    // Java guarantees exact sizes regardless of the host CPU - unlike C/C++.
    // -------------------------------------------------------------------------

    // Integer types
    byte  smallByte  = 127;           // 8-bit  signed: -128 to 127
    short mediumShort = 32_767;       // 16-bit signed: -32768 to 32767
    int   normalInt  = 2_147_483_647; // 32-bit signed: ~2.1 billion
    long  bigLong    = 9_223_372_036_854_775_807L; // 64-bit; note the L suffix

    // Floating point - IEEE 754
    float  singlePrecision = 3.14f;   // 32-bit; note the f suffix
    double doublePrecision = 3.141592653589793; // 64-bit; default for decimals

    // Other primitives
    char  letter    = 'A';            // 16-bit Unicode character (0–65535)
    boolean flag    = true;           // true or false - NOT 0/1 like C

    // -------------------------------------------------------------------------
    // LITERALS - the different ways to write values in source code
    // -------------------------------------------------------------------------
    static void demonstrateLiterals() {
        // Numeric literals can use underscores for readability (Java 7+)
        int million     = 1_000_000;
        long creditCard = 1234_5678_9012_3456L;

        // Different bases
        int decimal     = 255;
        int hex         = 0xFF;       // 0x prefix
        int octal       = 0377;       // 0 prefix
        int binary      = 0b1111_1111; // 0b prefix (Java 7+)

        // All four represent the same number
        System.out.println(decimal == hex);    // true
        System.out.println(decimal == octal);  // true
        System.out.println(decimal == binary); // true

        // String literals - stored in the String Pool (heap, but cached)
        String s1 = "hello";
        String s2 = "hello";
        String s3 = new String("hello"); // explicitly creates a new object

        System.out.println(s1 == s2);           // true  - same pool reference
        System.out.println(s1 == s3);           // false - different object
        System.out.println(s1.equals(s3));      // true  - same content
        // RULE: ALWAYS use .equals() to compare String content, never ==
    }

    // -------------------------------------------------------------------------
    // TYPE CASTING - widening (safe, automatic) vs narrowing (explicit, lossy)
    // -------------------------------------------------------------------------
    static void demonstrateCasting() {
        // WIDENING: smaller type → larger type. Automatic, no data loss.
        int i = 100_000;
        long l = i;           // int → long: implicit, always safe
        double d = l;         // long → double: implicit, but may lose precision
                              // for very large longs (>2^53)

        System.out.println("int: " + i + ", long: " + l + ", double: " + d);

        // NARROWING: larger type → smaller type. Requires explicit cast.
        // May silently truncate or change the value - this is a common bug.
        double pi     = 3.99999;
        int truncated = (int) pi;   // explicit cast - drops the decimal part
        System.out.println("Narrowed " + pi + " → " + truncated); // prints 3, not 4

        // Integer overflow: what happens when you exceed max value
        int max     = Integer.MAX_VALUE; // 2,147,483,647
        int wrapped = max + 1;           // wraps around to -2,147,483,648
        System.out.println("MAX_VALUE + 1 = " + wrapped); // negative! silent overflow

        // Safe overflow detection using Math.addExact
        try {
            int safe = Math.addExact(max, 1); // throws ArithmeticException
        } catch (ArithmeticException e) {
            System.out.println("Overflow caught: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // AUTOBOXING & UNBOXING - automatic conversion between primitives and wrappers
    // -------------------------------------------------------------------------
    static void demonstrateBoxing() {
        // Autoboxing: int → Integer (wrapper class)
        Integer boxed = 42;          // compiler inserts: Integer.valueOf(42)
        int unboxed   = boxed;       // unboxing: Integer → int

        // The Integer cache: valueOf() caches -128 to 127
        Integer a = 127;
        Integer b = 127;
        Integer c = 128;
        Integer d = 128;

        System.out.println(a == b); // true  - both point to cached object
        System.out.println(c == d); // false - outside cache range, new objects
        // This is a notorious interview gotcha and a real production bug source.
        // Always use .equals() for Integer comparisons.

        // Unboxing a null wrapper throws NullPointerException
        Integer nullableInt = null;
        try {
            int value = nullableInt; // NPE here - silent unboxing of null
        } catch (NullPointerException e) {
            System.out.println("Null unboxing NPE caught");
        }
    }

    // -------------------------------------------------------------------------
    // var - local variable type inference (Java 10+)
    // -------------------------------------------------------------------------
    static void demonstrateVar() {
        // var infers the type from the right-hand side at compile time.
        // It is NOT dynamic typing - the type is fixed at compile time.
        var count  = 0;          // inferred as int
        var name   = "Alice";    // inferred as String
        var list   = new java.util.ArrayList<String>(); // inferred as ArrayList<String>

        // var does NOT work for:
        // var x;          // no initializer
        // var x = null;   // cannot infer from null
        // method params, return types, or fields

        // Good use: when the type is obvious from the right-hand side
        var userMap = new java.util.HashMap<String, Integer>();
        // Bad use: when it obscures what the variable actually is
        // var result = processData(x); // - what type is result?
    }

    // -------------------------------------------------------------------------
    // CONSTANTS - static final
    // -------------------------------------------------------------------------
    // By convention: UPPER_SNAKE_CASE for compile-time constants
    static final int    MAX_RETRIES      = 3;
    static final double TAX_RATE         = 0.18;
    static final String DEFAULT_CURRENCY = "USD";
    // These are replaced by the compiler with their values - no runtime lookup.
    // Changing a public constant in a library requires recompiling all users.

    // -------------------------------------------------------------------------
    // SCOPE - where a variable is visible
    // -------------------------------------------------------------------------
    static void demonstrateScope() {
        int outer = 10;

        {
            // Block scope: 'inner' exists only inside these braces
            int inner = 20;
            System.out.println(outer + inner); // 30 - can see outer
        }
        // System.out.println(inner); // COMPILE ERROR: inner out of scope

        // Loop variable scope: 'i' exists only inside the for loop
        for (int i = 0; i < 3; i++) {
            System.out.println(i);
        }
        // System.out.println(i); // COMPILE ERROR
    }

    public static void main(String[] args) {
        demonstrateLiterals();
        demonstrateCasting();
        demonstrateBoxing();
        demonstrateVar();
        demonstrateScope();
    }
}
