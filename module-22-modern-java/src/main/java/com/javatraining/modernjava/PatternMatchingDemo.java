package com.javatraining.modernjava;

import java.util.*;

/**
 * Module 22 — Pattern Matching (Java 14–21)
 *
 * Pattern matching eliminates repetitive casting and null checks.
 *
 * Features by version:
 *   Java 14 preview / Java 16 final — instanceof pattern variable
 *   Java 17 preview / Java 21 final — sealed classes + switch patterns
 *   Java 21 final                   — record patterns, guarded patterns
 *
 * instanceof pattern (Java 16):
 *   if (obj instanceof String s) { use s directly }
 *
 * Switch expression (Java 14 final):
 *   String result = switch (x) {
 *       case 1 -> "one";
 *       default -> "other";
 *   };
 *
 * Switch with type patterns (Java 21):
 *   switch (obj) {
 *       case Integer i -> ...
 *       case String s  -> ...
 *       case null      -> ...
 *   }
 *
 * Record patterns (Java 21):
 *   if (obj instanceof Point(int x, int y)) { ... }
 *   case Circle(double r) -> ...
 *
 * Guarded patterns (Java 21):
 *   case String s when s.length() > 5 -> ...
 */
public class PatternMatchingDemo {

    // ── instanceof pattern variable (Java 16) ─────────────────────────────────

    /** Old style: explicit cast after instanceof. */
    public static String describeOld(Object obj) {
        if (obj instanceof String) {
            String s = (String) obj;   // explicit cast
            return "String of length " + s.length();
        }
        if (obj instanceof Integer) {
            Integer i = (Integer) obj;
            return "Integer: " + i;
        }
        return "unknown";
    }

    /** New style: pattern variable — no explicit cast needed. */
    public static String describe(Object obj) {
        if (obj instanceof String s)   return "String of length " + s.length();
        if (obj instanceof Integer i)  return "Integer: " + i;
        if (obj instanceof Double d)   return "Double: " + d;
        if (obj instanceof List<?> l)  return "List with " + l.size() + " elements";
        return "unknown";
    }

    // ── Pattern variable in compound conditions ───────────────────────────────

    /** Pattern variable is in scope for the rest of the &&-chain. */
    public static boolean isLongString(Object obj) {
        return obj instanceof String s && s.length() > 10;
    }

    public static boolean isPositiveInt(Object obj) {
        return obj instanceof Integer i && i > 0;
    }

    // ── Switch expression (Java 14+) ──────────────────────────────────────────

    /** Arrow-style switch expression (no fall-through, returns value). */
    public static String dayType(String day) {
        return switch (day.toUpperCase()) {
            case "SATURDAY", "SUNDAY" -> "weekend";
            case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "weekday";
            default -> "unknown";
        };
    }

    /** Switch expression with yield (for multi-statement arms). */
    public static int daysInMonth(int month, int year) {
        return switch (month) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31;
            case 4, 6, 9, 11            -> 30;
            case 2 -> {
                boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
                yield leap ? 29 : 28;
            }
            default -> throw new IllegalArgumentException("invalid month: " + month);
        };
    }

    // ── Sealed classes + switch patterns (Java 17 / 21) ──────────────────────

    /**
     * Sealed hierarchy: the compiler knows all permitted subtypes,
     * enabling exhaustive switch without a default clause.
     */
    public sealed interface Expr
            permits PatternMatchingDemo.Num,
                    PatternMatchingDemo.Add,
                    PatternMatchingDemo.Mul,
                    PatternMatchingDemo.Neg {}

    public record Num(double value) implements Expr {}
    public record Add(Expr left, Expr right) implements Expr {}
    public record Mul(Expr left, Expr right) implements Expr {}
    public record Neg(Expr expr) implements Expr {}

    /** Evaluates an expression tree using switch with type patterns + record patterns. */
    public static double eval(Expr expr) {
        return switch (expr) {
            case Num(double v)         -> v;
            case Add(Expr l, Expr r)   -> eval(l) + eval(r);
            case Mul(Expr l, Expr r)   -> eval(l) * eval(r);
            case Neg(Expr e)           -> -eval(e);
        };
    }

    /** Pretty-prints an expression tree. */
    public static String pretty(Expr expr) {
        return switch (expr) {
            case Num(double v)         -> String.valueOf(v);
            case Add(Expr l, Expr r)   -> "(" + pretty(l) + " + " + pretty(r) + ")";
            case Mul(Expr l, Expr r)   -> "(" + pretty(l) + " * " + pretty(r) + ")";
            case Neg(Expr e)           -> "(-" + pretty(e) + ")";
        };
    }

    // ── Switch with type patterns + guarded patterns (Java 21) ───────────────

    /** Classifies an object including guarded pattern (when clause). */
    public static String classify(Object obj) {
        return switch (obj) {
            case null                           -> "null";
            case Integer i when i < 0          -> "negative int: " + i;
            case Integer i when i == 0         -> "zero";
            case Integer i                     -> "positive int: " + i;
            case String s when s.isBlank()     -> "blank string";
            case String s                      -> "string: \"" + s + "\"";
            case Double d                      -> "double: " + d;
            case int[] arr                     -> "int array of length " + arr.length;
            default                            -> "other: " + obj.getClass().getSimpleName();
        };
    }

    // ── Sealed class hierarchies ──────────────────────────────────────────────

    /** HTTP response modelled as a sealed type. */
    public sealed interface HttpResult
            permits PatternMatchingDemo.Ok,
                    PatternMatchingDemo.NotFound,
                    PatternMatchingDemo.ServerError {}

    public record Ok(String body) implements HttpResult {}
    public record NotFound(String path) implements HttpResult {}
    public record ServerError(int code, String message) implements HttpResult {}

    public static String handleResult(HttpResult result) {
        return switch (result) {
            case Ok(String body)                    -> "200 OK: " + body;
            case NotFound(String path)              -> "404 Not Found: " + path;
            case ServerError(int code, String msg)  -> code + " Error: " + msg;
        };
    }

    // ── Record patterns nested ────────────────────────────────────────────────

    public record Point(int x, int y) {}
    public record Line(Point start, Point end) {}

    /** Destructures nested record patterns. */
    public static String describeLine(Object obj) {
        return switch (obj) {
            case Line(Point(int x1, int y1), Point(int x2, int y2)) ->
                String.format("(%d,%d) → (%d,%d)", x1, y1, x2, y2);
            default -> "not a line";
        };
    }

    // ── Null handling in switch (Java 21) ────────────────────────────────────

    public static String nullSafeClassify(Object obj) {
        return switch (obj) {
            case null      -> "null value";
            case String s  -> "string: " + s;
            default        -> "other";
        };
    }
}
