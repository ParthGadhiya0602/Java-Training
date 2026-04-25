package com.javatraining.methods;

import java.util.Arrays;

/**
 * TOPIC: Varargs - variable-length argument lists.
 *
 * Covers:
 * - Basic varargs syntax and internal array representation
 * - Rules: must be last, only one per method
 * - Calling with 0, N arguments, or an explicit array
 * - Null safety
 * - Varargs + overloading resolution order
 * - Practical: printf-style message formatting, bulk aggregation
 */
public class VarargsDemo {

    // -------------------------------------------------------------------------
    // Basic varargs - int... is just a syntactic sugar for int[]
    // -------------------------------------------------------------------------
    static int sum(int... numbers) {
        // Inside the method body, 'numbers' is an int[]
        int total = 0;
        for (int n : numbers) total += n;
        return total;
    }

    static double average(double... values) {
        if (values == null || values.length == 0)
            throw new IllegalArgumentException("At least one value required");
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    // -------------------------------------------------------------------------
    // Varargs must be the LAST parameter
    // -------------------------------------------------------------------------
    static String join(String separator, String... parts) {
        // 'separator' is a normal fixed param; 'parts' is the vararg at the end
        if (parts == null || parts.length == 0) return "";
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(separator).append(parts[i]);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Null safety - caller can pass null explicitly as the array
    // -------------------------------------------------------------------------
    static void printAll(String... messages) {
        if (messages == null) {
            System.out.println("(null passed as vararg)");
            return;
        }
        for (String m : messages) {
            System.out.println(m != null ? m : "(null element)");
        }
    }

    // -------------------------------------------------------------------------
    // Varargs + overloading - exact match wins over varargs
    // -------------------------------------------------------------------------
    static String detect(String s)       { return "exact String";  }
    static String detect(String... ss)   { return "varargs String[" + ss.length + "]"; }

    // -------------------------------------------------------------------------
    // Practical: a type-safe message builder
    // Works like String.format but throws immediately on argument count mismatch
    // -------------------------------------------------------------------------
    static String build(String template, Object... args) {
        // Count how many {} placeholders are in the template
        int placeholders = 0;
        for (int i = 0; i < template.length() - 1; i++) {
            if (template.charAt(i) == '{' && template.charAt(i + 1) == '}')
                placeholders++;
        }
        if (placeholders != args.length)
            throw new IllegalArgumentException(
                "Template has " + placeholders + " placeholders but " +
                args.length + " args were provided");

        // Replace each {} with the corresponding arg
        String result = template;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", arg == null ? "null" : arg.toString());
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Practical: statistics aggregator
    // -------------------------------------------------------------------------
    record Stats(int count, double min, double max, double sum, double average) {
        @Override public String toString() {
            return String.format(
                "Stats{count=%d, min=%.2f, max=%.2f, sum=%.2f, avg=%.2f}",
                count, min, max, sum, average);
        }
    }

    static Stats stats(double first, double... rest) {
        // Requiring at least one argument via 'first' is a common varargs pattern
        // that prevents an empty call and avoids the null/empty check
        double min = first, max = first, sum = first;
        int count = 1;

        for (double v : rest) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
            count++;
        }
        return new Stats(count, min, max, sum, sum / count);
    }

    public static void main(String[] args) {
        System.out.println("=== sum() with varargs ===");
        System.out.println(sum());              // 0  - empty vararg is legal
        System.out.println(sum(5));             // 5
        System.out.println(sum(1, 2, 3));       // 6
        System.out.println(sum(10, 20, 30, 40, 50)); // 150

        // Can pass an explicit array - varargs and array are interchangeable
        int[] data = {3, 6, 9};
        System.out.println(sum(data));          // 18

        System.out.println("\n=== join() with fixed + varargs params ===");
        System.out.println(join(", ", "Alice", "Bob", "Charlie")); // Alice, Bob, Charlie
        System.out.println(join(" | ", "Mumbai", "Delhi"));        // Mumbai | Delhi
        System.out.println(join("-"));                             // "" (empty)

        System.out.println("\n=== Null safety ===");
        printAll("hello", null, "world");  // hello, (null element), world
        printAll();                        // nothing printed
        printAll((String[]) null);         // (null passed as vararg)

        System.out.println("\n=== Overload resolution: exact vs varargs ===");
        System.out.println(detect("one"));         // exact String  (exact > varargs)
        System.out.println(detect("one", "two"));  // varargs String[2]
        System.out.println(detect());              // varargs String[0]

        System.out.println("\n=== build() template ===");
        System.out.println(build("Hello, {}! You have {} messages.", "Alice", 5));
        System.out.println(build("Order {} placed at {} for ₹{}", "ORD-001", "10:30", 1299.50));
        try {
            build("Hello, {}!", "Alice", "extra"); // mismatch → exception
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n=== stats() - at-least-one pattern ===");
        System.out.println(stats(5.0));
        System.out.println(stats(3.0, 1.5, 7.2, 4.8, 9.1));
        System.out.println(stats(100.0, 200.0, 300.0));
    }
}
