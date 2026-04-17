package com.javatraining.testing;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Pure string utility functions — ideal subjects for parameterized tests because
 * each method maps a single input to a deterministic output with no side effects.
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * Returns {@code true} if the string reads the same forwards and backwards,
     * ignoring case, spaces, and non-alphanumeric characters.
     */
    public static boolean isPalindrome(String s) {
        if (s == null) return false;
        String cleaned = s.toLowerCase().replaceAll("[^a-z0-9]", "");
        return cleaned.equals(new StringBuilder(cleaned).reverse().toString());
    }

    /** Counts whitespace-separated words; returns 0 for null or blank input. */
    public static int countWords(String s) {
        if (s == null || s.isBlank()) return 0;
        return s.strip().split("\\s+").length;
    }

    /**
     * Title-cases every whitespace-separated word.
     * Returns the input unchanged if it is {@code null} or empty.
     */
    public static String capitalize(String s) {
        if (s == null) return null;
        if (s.isEmpty()) return s;
        return Arrays.stream(s.split("\\s+"))
            .map(w -> w.isEmpty() ? w
                    : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    /**
     * Truncates {@code s} to at most {@code maxLength} characters.
     * Strings that exceed the limit are cut and suffixed with {@code "..."}.
     *
     * @throws IllegalArgumentException if {@code maxLength < 3}
     */
    public static String truncate(String s, int maxLength) {
        if (s == null) return null;
        if (maxLength < 3) throw new IllegalArgumentException("maxLength must be >= 3");
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }

    /**
     * Returns {@code true} if both strings contain the same characters
     * in any order, ignoring case and non-alphabetic characters.
     */
    public static boolean isAnagram(String a, String b) {
        if (a == null || b == null) return false;
        char[] ca = a.toLowerCase().replaceAll("[^a-z]", "").toCharArray();
        char[] cb = b.toLowerCase().replaceAll("[^a-z]", "").toCharArray();
        Arrays.sort(ca);
        Arrays.sort(cb);
        return Arrays.equals(ca, cb);
    }

    /** Returns the words of the string in reverse order. */
    public static String reverseWords(String s) {
        if (s == null || s.isBlank()) return s;
        String[] words = s.strip().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }
}
