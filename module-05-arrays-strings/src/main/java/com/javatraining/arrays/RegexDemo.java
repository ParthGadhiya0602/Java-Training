package com.javatraining.arrays;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TOPIC: Regular Expressions - Pattern, Matcher, groups, named groups,
 * common patterns, and production best practices.
 *
 * KEY RULE: Compile Pattern objects once as static finals.
 * Pattern.compile() parses the regex - doing it on every call wastes CPU.
 */
public class RegexDemo {

    // -------------------------------------------------------------------------
    // Pre-compiled patterns - static finals so compilation happens ONCE
    // -------------------------------------------------------------------------

    // Email: simplified but covers 99% of real addresses
    private static final Pattern EMAIL = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    // Indian mobile: starts 6-9, followed by 9 more digits
    private static final Pattern MOBILE_IN = Pattern.compile(
        "^[6-9]\\d{9}$"
    );

    // Date: YYYY-MM-DD with basic month/day validation
    private static final Pattern DATE_YMD = Pattern.compile(
        "^(?<year>\\d{4})-(?<month>0[1-9]|1[0-2])-(?<day>0[1-9]|[12]\\d|3[01])$"
    );

    // Integer (with optional sign)
    private static final Pattern INTEGER = Pattern.compile("^-?\\d+$");

    // Decimal number
    private static final Pattern DECIMAL = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    // URL (basic http/https)
    private static final Pattern URL = Pattern.compile(
        "https?://[\\w\\-.]+(?:/[\\w\\-./?%&=#]*)?",
        Pattern.CASE_INSENSITIVE
    );

    // HTML tag (for stripping)
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    // One or more whitespace characters (for normalisation)
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");

    // -------------------------------------------------------------------------
    // 1. matches() vs find()
    // -------------------------------------------------------------------------
    static void matchesVsFind() {
        String text = "My email is alice@example.com - please write to me.";

        // matches() - checks the ENTIRE string against the pattern
        System.out.println("matches full string: " + EMAIL.matcher(text).matches()); // false

        // find() - searches for the pattern ANYWHERE in the string
        Matcher m = EMAIL.matcher(text);
        if (m.find()) {
            System.out.println("found in string:     " + m.group());  // alice@example.com
            System.out.println("  start=" + m.start() + " end=" + m.end());
        }

        // Calling find() again on the same Matcher continues from where it left off
        String multi = "Contact: a@b.com or c@d.org for help.";
        Matcher all = EMAIL.matcher(multi);
        System.out.print("All emails: ");
        while (all.find()) System.out.print(all.group() + "  ");
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // 2. Validation helpers
    // -------------------------------------------------------------------------
    static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email).matches();
    }

    static boolean isValidMobile(String mobile) {
        return mobile != null && MOBILE_IN.matcher(mobile.replaceAll("\\s", "")).matches();
    }

    static boolean isInteger(String s) {
        return s != null && INTEGER.matcher(s.strip()).matches();
    }

    static boolean isDecimal(String s) {
        return s != null && DECIMAL.matcher(s.strip()).matches();
    }

    // -------------------------------------------------------------------------
    // 3. Capturing groups - extract sub-parts of a match
    // -------------------------------------------------------------------------
    static void capturingGroups() {
        // Extract date parts using numbered groups
        Pattern numbered = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
        String log = "Deployed on 2024-04-15 at 10:30";
        Matcher m = numbered.matcher(log);

        if (m.find()) {
            System.out.println("\nNumbered groups:");
            System.out.println("  Full match : " + m.group(0)); // 2024-04-15
            System.out.println("  Year       : " + m.group(1)); // 2024
            System.out.println("  Month      : " + m.group(2)); // 04
            System.out.println("  Day        : " + m.group(3)); // 15
        }

        // Named groups - self-documenting, order-independent
        Matcher nm = DATE_YMD.matcher("2024-04-15");
        if (nm.matches()) {
            System.out.println("\nNamed groups:");
            System.out.println("  year  = " + nm.group("year"));   // 2024
            System.out.println("  month = " + nm.group("month"));  // 04
            System.out.println("  day   = " + nm.group("day"));    // 15
        }
    }

    // -------------------------------------------------------------------------
    // 4. Back-references in replacements - rearrange captured groups
    // -------------------------------------------------------------------------
    static void backreferences() {
        // Convert YYYY-MM-DD to DD/MM/YYYY
        String date = "2024-04-15";
        String rearranged = date.replaceAll(
            "(\\d{4})-(\\d{2})-(\\d{2})",
            "$3/$2/$1"
        );
        System.out.println("\nDate rearranged: " + rearranged); // 15/04/2024

        // Wrap every number in brackets
        String sentence = "I have 3 cats and 5 dogs.";
        String wrapped = sentence.replaceAll("(\\d+)", "[$1]");
        System.out.println("Wrapped numbers: " + wrapped); // I have [3] cats and [5] dogs.
    }

    // -------------------------------------------------------------------------
    // 5. Practical transformations
    // -------------------------------------------------------------------------
    static String normaliseWhitespace(String text) {
        return WHITESPACE_RUN.matcher(text.strip()).replaceAll(" ");
    }

    static String stripHtml(String html) {
        return HTML_TAG.matcher(html).replaceAll("").strip();
    }

    static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<>();
        Matcher m = URL.matcher(text);
        while (m.find()) urls.add(m.group());
        return urls;
    }

    // -------------------------------------------------------------------------
    // 6. split() with regex - more powerful than plain String.split
    // -------------------------------------------------------------------------
    static void splitWithRegex() {
        // Split on one-or-more whitespace
        String sentence = "  one   two\tthree\n  four  ";
        String[] tokens = sentence.strip().split("\\s+");
        System.out.println("\nTokens: " + java.util.Arrays.toString(tokens));
        // [one, two, three, four]

        // Split CSV that may have spaces around commas
        String csv = "Alice , 30 , Mumbai , Engineer";
        String[] fields = csv.split("\\s*,\\s*");
        System.out.println("CSV fields: " + java.util.Arrays.toString(fields));
        // [Alice, 30, Mumbai, Engineer]

        // Split preserving the delimiter using lookahead
        String camel = "myVariableName";
        String[] words = camel.split("(?=[A-Z])"); // split BEFORE each uppercase letter
        System.out.println("CamelCase words: " + java.util.Arrays.toString(words));
        // [my, Variable, Name]
    }

    // -------------------------------------------------------------------------
    // 7. Flags - CASE_INSENSITIVE, MULTILINE, DOTALL
    // -------------------------------------------------------------------------
    static void patternFlags() {
        // CASE_INSENSITIVE - match regardless of case
        Pattern p = Pattern.compile("hello", Pattern.CASE_INSENSITIVE);
        System.out.println("\nCase insensitive: " + p.matcher("HELLO World").find()); // true

        // MULTILINE - ^ and $ match start/end of each LINE (not just the whole string)
        Pattern multiLine = Pattern.compile("^\\d+", Pattern.MULTILINE);
        String text = "100 apples\n200 oranges\n300 bananas";
        Matcher m = multiLine.matcher(text);
        System.out.print("Line starts with number: ");
        while (m.find()) System.out.print(m.group() + " ");
        System.out.println();  // 100 200 300

        // DOTALL - . also matches newlines (by default it doesn't)
        String block = "START\nsome content\nEND";
        System.out.println("DOTALL match: " +
            Pattern.compile("START.*END", Pattern.DOTALL).matcher(block).find()); // true
        System.out.println("without DOTALL: " +
            Pattern.compile("START.*END").matcher(block).find()); // false
    }

    public static void main(String[] args) {
        System.out.println("=== matches() vs find() ===");
        matchesVsFind();

        System.out.println("\n=== Validation ===");
        String[] emails = {"alice@example.com", "bad@", "@nodomain", "user.name+tag@domain.co.in"};
        for (String e : emails)
            System.out.printf("  %-30s → %s%n", e, isValidEmail(e) ? "valid" : "invalid");

        System.out.println();
        String[] phones = {"9876543210", "1234567890", "98765", "+91 9876543210"};
        for (String p : phones)
            System.out.printf("  %-20s → %s%n", p, isValidMobile(p) ? "valid" : "invalid");

        capturingGroups();
        backreferences();

        System.out.println("\n=== Transformations ===");
        System.out.println(normaliseWhitespace("  too   many    spaces   "));
        System.out.println(stripHtml("<h1>Hello</h1> <p>World <b>!</b></p>"));

        String html = "Visit https://java.com or http://docs.oracle.com/javase for docs.";
        System.out.println("URLs: " + extractUrls(html));

        splitWithRegex();
        patternFlags();
    }
}
