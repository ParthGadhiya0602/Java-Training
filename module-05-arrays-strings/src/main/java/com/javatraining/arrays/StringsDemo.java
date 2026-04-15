package com.javatraining.arrays;

import java.util.StringJoiner;

/**
 * TOPIC: String immutability, String pool, full String API,
 * StringBuilder, StringJoiner, and Text Blocks.
 */
public class StringsDemo {

    // -------------------------------------------------------------------------
    // Immutability — every "modifying" operation returns a NEW String
    // -------------------------------------------------------------------------
    static void immutability() {
        String s = "hello";
        s.toUpperCase();                  // result discarded — s unchanged
        System.out.println("After toUpperCase() without assignment: " + s); // hello

        String upper = s.toUpperCase();   // capture the new String
        System.out.println("Captured result: " + upper);  // HELLO
        System.out.println("Original still: " + s);       // hello

        // Concatenation with + also creates a new String
        String a = "foo";
        String b = a;               // both point to the same object
        a = a + "bar";              // a now points to a NEW "foobar" String
        System.out.println("a=" + a + ", b=" + b); // a=foobar, b=foo (b unchanged)
    }

    // -------------------------------------------------------------------------
    // String Pool — == vs equals()
    // -------------------------------------------------------------------------
    static void stringPool() {
        String s1 = "java";                    // pool
        String s2 = "java";                    // same pool entry
        String s3 = new String("java");        // new heap object, bypasses pool
        String s4 = s3.intern();               // intern: put in pool, return pool ref

        System.out.println("s1 == s2: " + (s1 == s2));   // true  — same pool ref
        System.out.println("s1 == s3: " + (s1 == s3));   // false — different objects
        System.out.println("s1 == s4: " + (s1 == s4));   // true  — s4 is interned

        // Compile-time constant folding
        String s5 = "ja" + "va";               // compiler folds to "java" at compile time
        System.out.println("s1 == s5: " + (s1 == s5));   // true — compiler-folded

        String part = "ja";
        String s6 = part + "va";               // runtime concat — new heap object
        System.out.println("s1 == s6: " + (s1 == s6));   // false — runtime result
        System.out.println("s1.equals(s6): " + s1.equals(s6)); // true — same content
    }

    // -------------------------------------------------------------------------
    // String API
    // -------------------------------------------------------------------------
    static void stringAPI() {
        String s = "  Hello, World!  ";

        // Inspection
        System.out.println("length:         " + s.length());          // 18
        System.out.println("isEmpty:        " + s.isEmpty());         // false
        System.out.println("isBlank:        " + s.isBlank());         // false
        System.out.println("\"   \".isBlank: " + "   ".isBlank());     // true (Java 11+)
        System.out.println("charAt(2):      " + s.charAt(2));         // 'H'
        System.out.println("indexOf('o'):   " + s.indexOf('o'));       // 4
        System.out.println("lastIndexOf:    " + s.lastIndexOf('o'));   // 9
        System.out.println("contains World: " + s.contains("World")); // true
        System.out.println("startsWith '  H': " + s.startsWith("  H")); // true

        // Extraction
        System.out.println("\nsubstring(9):       " + s.substring(9));       // "orld!  "
        System.out.println("substring(9, 14):   " + s.substring(9, 14));    // "orld!"

        // Transformation
        System.out.println("\ntoLowerCase:        " + s.toLowerCase());
        System.out.println("toUpperCase:        " + s.toUpperCase());
        System.out.println("trim():             '" + s.trim() + "'");        // ASCII whitespace
        System.out.println("strip():            '" + s.strip() + "'");       // Unicode-aware
        System.out.println("stripLeading():     '" + s.stripLeading() + "'");
        System.out.println("replace('l','r'):   " + s.replace('l', 'r'));
        System.out.println("replace World:      " + s.replace("World", "Java"));
        System.out.println("replaceAll \\s+:    " + s.replaceAll("\\s+", "_"));

        // Split & Join
        System.out.println("\nSplit by ', ':");
        String csv = "Alice,30,Mumbai,Engineer";
        String[] parts = csv.split(",");
        for (String p : parts) System.out.println("  → " + p);

        System.out.println("join: " + String.join(" | ", parts));

        // Conversion
        System.out.println("\nvalueOf(42):    " + String.valueOf(42));
        System.out.println("parseInt:       " + Integer.parseInt("  42  ".strip()));
        System.out.println("format:         " + String.format("%-10s %5d", "item", 42));

        // Java 11+
        System.out.println("\nrepeat:         " + "ab".repeat(4));    // ababababab
        System.out.println("lines count:    " +
            "a\nb\nc".lines().count());   // 3

        // Comparison
        System.out.println("\ncompareTo:      " + "apple".compareTo("banana")); // negative
        System.out.println("equalsIgnoreCase: " + "Java".equalsIgnoreCase("JAVA")); // true
    }

    // -------------------------------------------------------------------------
    // String concatenation performance
    // -------------------------------------------------------------------------
    static void concatenationPerformance() {
        int iterations = 50_000;

        // Bad: + in a loop
        long start = System.nanoTime();
        String bad = "";
        for (int i = 0; i < iterations; i++) bad += "x";
        long badTime = System.nanoTime() - start;

        // Good: StringBuilder
        start = System.nanoTime();
        StringBuilder sb = new StringBuilder(iterations);
        for (int i = 0; i < iterations; i++) sb.append("x");
        String good = sb.toString();
        long goodTime = System.nanoTime() - start;

        System.out.printf("\n%,d iterations:%n", iterations);
        System.out.printf("  String +       : %,d ms%n", badTime  / 1_000_000);
        System.out.printf("  StringBuilder  : %,d ms%n", goodTime / 1_000_000);
        System.out.printf("  Speedup factor : %.0fx%n", (double) badTime / goodTime);
    }

    // -------------------------------------------------------------------------
    // StringBuilder API
    // -------------------------------------------------------------------------
    static void stringBuilderAPI() {
        StringBuilder sb = new StringBuilder();

        // Chaining — each method returns 'this'
        sb.append("Hello")
          .append(", ")
          .append("World")
          .append('!');
        System.out.println("\nAfter appends:  " + sb);       // Hello, World!

        sb.insert(5, " Beautiful");
        System.out.println("After insert:   " + sb);         // Hello Beautiful, World!

        sb.delete(5, 15);
        System.out.println("After delete:   " + sb);         // Hello, World!

        sb.replace(7, 12, "Java");
        System.out.println("After replace:  " + sb);         // Hello, Java!

        System.out.println("reverse:        " + new StringBuilder("abcde").reverse()); // edcba

        // charAt and indexOf
        System.out.println("charAt(0):      " + sb.charAt(0));     // 'H'
        System.out.println("indexOf Java:   " + sb.indexOf("Java")); // 7
        System.out.println("length:         " + sb.length());

        // capacity — internal buffer, resizes automatically
        StringBuilder sized = new StringBuilder(256);   // pre-allocated
        System.out.println("initial capacity: " + sized.capacity()); // 256
    }

    // -------------------------------------------------------------------------
    // StringJoiner
    // -------------------------------------------------------------------------
    static void stringJoinerAPI() {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        sj.add("Alice");
        sj.add("Bob");
        sj.add("Carol");
        System.out.println("\nStringJoiner:   " + sj);          // [Alice, Bob, Carol]

        // Empty value
        StringJoiner empty = new StringJoiner(", ", "[", "]");
        empty.setEmptyValue("(none)");
        System.out.println("Empty joiner:   " + empty);         // (none)

        // merge — append another StringJoiner's content
        StringJoiner first  = new StringJoiner(", ");
        StringJoiner second = new StringJoiner(", ");
        first.add("A").add("B");
        second.add("C").add("D");
        first.merge(second);
        System.out.println("Merged:         " + first);         // A, B, C, D

        // String.join shorthand
        System.out.println("String.join:    " +
            String.join(" → ", "Step1", "Step2", "Step3"));     // Step1 → Step2 → Step3
    }

    // -------------------------------------------------------------------------
    // Text Blocks (Java 13+)
    // -------------------------------------------------------------------------
    static void textBlocks() {
        // Multi-line JSON without escape hell
        String json = """
                {
                  "name": "Alice",
                  "age": 30,
                  "city": "Mumbai"
                }
                """;
        System.out.println("\nText block JSON:");
        System.out.println(json);

        // SQL becomes readable
        String sql = """
                SELECT u.name, u.email
                FROM   users u
                JOIN   orders o ON u.id = o.user_id
                WHERE  o.status = 'PENDING'
                ORDER  BY o.created_at DESC
                """;
        System.out.println("SQL query:");
        System.out.println(sql);

        // Formatted text block (Java 14+) — uses String.format-style %s
        String name = "Bob";
        int count = 5;
        String msg = """
                Dear %s,
                You have %d pending notifications.
                """.formatted(name, count);
        System.out.println(msg);
    }

    public static void main(String[] args) {
        System.out.println("=== Immutability ===");
        immutability();

        System.out.println("\n=== String Pool ===");
        stringPool();

        System.out.println("\n=== String API ===");
        stringAPI();

        concatenationPerformance();

        System.out.println("\n=== StringBuilder ===");
        stringBuilderAPI();

        stringJoinerAPI();
        textBlocks();
    }
}
