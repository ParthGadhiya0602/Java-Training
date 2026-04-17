package com.javatraining.modernjava;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Module 22 — Records (Java 16+)
 *
 * A record is a transparent, immutable data carrier.
 * The compiler auto-generates:
 *   - private final fields for each component
 *   - public accessor methods (name(), age(), ...)  — NOT getXxx()
 *   - canonical constructor
 *   - equals(), hashCode(), toString()
 *
 * Records cannot:
 *   - extend another class (they implicitly extend java.lang.Record)
 *   - declare instance fields outside the record header
 *   - be abstract
 *
 * Records can:
 *   - implement interfaces
 *   - define static fields and methods
 *   - define instance methods
 *   - add compact, custom, or canonical constructors
 *   - be generic
 *   - be local (declared inside a method)
 */
public class RecordsDemo {

    // ── Basic record ──────────────────────────────────────────────────────────

    public record Point(double x, double y) {
        // Static factory
        public static Point origin() { return new Point(0, 0); }

        // Instance method
        public double distanceTo(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        public Point translate(double dx, double dy) {
            return new Point(x + dx, y + dy);
        }
    }

    // ── Compact constructor (validation) ─────────────────────────────────────

    /**
     * Compact constructor: no parameter list, no explicit assignments.
     * The compiler adds the assignments at the end automatically.
     * Use for validation and normalisation.
     */
    public record Range(int min, int max) {
        public Range {   // compact constructor
            if (min > max) throw new IllegalArgumentException(
                "min " + min + " > max " + max);
        }

        public boolean contains(int value) { return value >= min && value <= max; }
        public int     size()              { return max - min; }
    }

    // ── Canonical constructor override ───────────────────────────────────────

    /** Normalise the name in the canonical constructor. */
    public record Person(String name, int age) {
        public Person(String name, int age) {  // canonical constructor
            this.name = name.strip();          // normalise whitespace
            this.age  = age;
            if (age < 0) throw new IllegalArgumentException("age must be >= 0");
        }

        public String greeting() { return "Hi, I'm " + name + " (" + age + ")"; }
    }

    // ── Generic record ────────────────────────────────────────────────────────

    public record Pair<A, B>(A first, B second) {
        public Pair<B, A> swap() { return new Pair<>(second, first); }

        public static <T> Pair<T, T> of(T value) { return new Pair<>(value, value); }
    }

    // ── Record implementing an interface ─────────────────────────────────────

    public interface Shape {
        double area();
        double perimeter();
    }

    public record Circle(double radius) implements Shape {
        public Circle {
            if (radius <= 0) throw new IllegalArgumentException("radius must be positive");
        }
        @Override public double area()      { return Math.PI * radius * radius; }
        @Override public double perimeter() { return 2 * Math.PI * radius; }
    }

    public record Rectangle(double width, double height) implements Shape {
        public Rectangle {
            if (width <= 0 || height <= 0)
                throw new IllegalArgumentException("dimensions must be positive");
        }
        @Override public double area()      { return width * height; }
        @Override public double perimeter() { return 2 * (width + height); }
    }

    // ── Static members in records ─────────────────────────────────────────────

    public record Temperature(double celsius) {
        public static final double ABSOLUTE_ZERO = -273.15;

        public double toFahrenheit() { return celsius * 9.0 / 5.0 + 32; }
        public double toKelvin()     { return celsius - ABSOLUTE_ZERO; }

        public static Temperature fromFahrenheit(double f) {
            return new Temperature((f - 32) * 5.0 / 9.0);
        }

        public boolean isFreezing() { return celsius <= 0; }
    }

    // ── Utility methods using records ─────────────────────────────────────────

    /** Returns the centroid of a list of points. */
    public static Point centroid(List<Point> points) {
        if (points.isEmpty()) throw new IllegalArgumentException("empty list");
        double avgX = points.stream().mapToDouble(Point::x).average().orElse(0);
        double avgY = points.stream().mapToDouble(Point::y).average().orElse(0);
        return new Point(avgX, avgY);
    }

    /** Filters shapes whose area exceeds the threshold. */
    public static List<Shape> largShapes(List<Shape> shapes, double minArea) {
        return shapes.stream()
            .filter(s -> s.area() > minArea)
            .collect(Collectors.toList());
    }

    /** Groups persons by age bracket (decade). */
    public static Map<Integer, List<Person>> groupByDecade(List<Person> people) {
        return people.stream()
            .collect(Collectors.groupingBy(p -> (p.age() / 10) * 10));
    }
}
