package com.javatraining.methods;

/**
 * TOPIC: Method overloading and how the compiler resolves overloaded calls.
 *
 * Demonstrates:
 * - Same name, multiple parameter lists
 * - Overload resolution: exact > widening > autoboxing > varargs
 * - Widening trap with numeric types
 * - Null argument ambiguity
 */
public class OverloadingDemo {

    // -------------------------------------------------------------------------
    // A real-world overloading example: formatting values for display
    // -------------------------------------------------------------------------
    public static String format(int value) {
        return String.format("%,d", value);              // "1,234,567"
    }

    public static String format(double value) {
        return String.format("%,.2f", value);            // "1,234.56"
    }

    public static String format(double value, String unit) {
        return String.format("%,.2f %s", value, unit);  // "1,234.56 km"
    }

    public static String format(boolean value) {
        return value ? "Yes" : "No";
    }

    public static String format(Object value) {
        if (value == null) return "-";
        return value.toString();
    }

    // -------------------------------------------------------------------------
    // Overload resolution demonstration
    // -------------------------------------------------------------------------

    // Four overloads - compiler picks one based on argument type
    static String whichOverload(int n)     { return "int";     }
    static String whichOverload(long n)    { return "long";    }
    static String whichOverload(double n)  { return "double";  }
    static String whichOverload(Integer n) { return "Integer"; }

    static void demonstrateResolution() {
        byte b = 10;
        System.out.println("byte arg   → " + whichOverload(b));    // int (widened from byte)
        System.out.println("int arg    → " + whichOverload(5));     // int (exact)
        System.out.println("long arg   → " + whichOverload(5L));    // long (exact)
        System.out.println("double arg → " + whichOverload(5.0));   // double (exact)
        System.out.println("Integer arg→ " + whichOverload(Integer.valueOf(5))); // Integer (exact)
        System.out.println("int literal→ " + whichOverload(5));
        // int wins over Integer for literal 5 - exact match before autoboxing
    }

    // -------------------------------------------------------------------------
    // Null ambiguity - a compile-time problem
    // -------------------------------------------------------------------------
    static void printName(String name)     { System.out.println("String: "  + name); }
    static void printName(StringBuilder sb){ System.out.println("SB: " + sb); }

    static void demonstrateNullAmbiguity() {
        printName("Alice");                    // String - exact match
        printName(new StringBuilder("Bob"));   // StringBuilder - exact match
        // printName(null);   ← COMPILE ERROR: ambiguous - both accept null
        // Resolve by casting:
        printName((String) null);              // explicitly choose String overload
    }

    // -------------------------------------------------------------------------
    // Overloading for a Logger utility - realistic example
    // -------------------------------------------------------------------------
    static void log(String message) {
        System.out.printf("[INFO ] %s%n", message);
    }

    static void log(String message, Object... context) {
        System.out.printf("[INFO ] " + message + "%n", context);
    }

    static void log(String level, String message) {
        System.out.printf("[%-5s] %s%n", level, message);
    }

    static void log(String level, String message, Throwable cause) {
        System.out.printf("[%-5s] %s | caused by: %s%n",
            level, message, cause.getMessage());
    }

    public static void main(String[] args) {
        System.out.println("=== format() overloads ===");
        System.out.println(format(1_234_567));
        System.out.println(format(9876.543));
        System.out.println(format(12_500.0, "₹"));
        System.out.println(format(true));
        System.out.println(format((Object) null));

        System.out.println("\n=== Overload Resolution ===");
        demonstrateResolution();

        System.out.println("\n=== Null Ambiguity ===");
        demonstrateNullAmbiguity();

        System.out.println("\n=== Logger overloads ===");
        log("Application started");
        log("User %s logged in from %s", "alice", "192.168.1.1");
        log("ERROR", "Database connection failed");
        log("WARN",  "Retry %d of %d", 2, 3);
        log("ERROR", "Unhandled exception", new RuntimeException("timeout"));
    }
}
