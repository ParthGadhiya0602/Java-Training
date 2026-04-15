package com.javatraining.inheritance;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TOPIC: Full design — Abstract class (Template Method) + sealed hierarchy
 *        + polymorphic processing pipeline
 *
 * Design:
 *   AbstractShape — abstract class providing the Template Method pattern.
 *     • area() and perimeter() are abstract — subclasses implement them.
 *     • describe(), scale(), and compareTo() are concrete template methods
 *       that call the abstract hooks.
 *
 *   Sealed interface Shape2D — restricts the set of concrete shapes so that
 *     switch expressions on shapes are exhaustive.
 *
 *   ShapeCalculator — processes a mixed list of shapes polymorphically:
 *     total area, largest shape, group by type, scale all by factor.
 */
public class ShapeCalculator {

    // -------------------------------------------------------------------------
    // 1. Abstract base class — Template Method pattern
    // -------------------------------------------------------------------------
    static abstract class AbstractShape implements Comparable<AbstractShape> {

        private final String color;

        AbstractShape(String color) {
            this.color = color;
        }

        // Abstract hooks — subclasses MUST implement
        abstract double area();
        abstract double perimeter();
        abstract String shapeName();

        // Template method — uses the hooks, cannot be overridden
        final String describe() {
            return String.format("%-12s color=%-8s area=%8.2f perimeter=%8.2f",
                shapeName(), color, area(), perimeter());
        }

        // Template method — creates a scaled copy
        final AbstractShape scaled(double factor) {
            if (factor <= 0) throw new IllegalArgumentException("Scale factor must be > 0");
            return scaleBy(factor);
        }

        // Hook for scaling — subclasses produce a new instance with scaled dimensions
        protected abstract AbstractShape scaleBy(double factor);

        String color() { return color; }

        // Natural ordering by area (descending)
        @Override
        public int compareTo(AbstractShape other) {
            return Double.compare(other.area(), this.area()); // descending
        }

        @Override
        public String toString() { return describe(); }
    }

    // -------------------------------------------------------------------------
    // 2. Concrete shapes — sealed so switch can be exhaustive
    // -------------------------------------------------------------------------
    static final class Circle extends AbstractShape {
        private final double radius;

        Circle(double radius, String color) {
            super(color);
            if (radius <= 0) throw new IllegalArgumentException("radius > 0 required");
            this.radius = radius;
        }

        @Override public double area()      { return Math.PI * radius * radius; }
        @Override public double perimeter() { return 2 * Math.PI * radius; }
        @Override public String shapeName() { return "Circle"; }
        @Override protected AbstractShape scaleBy(double f) {
            return new Circle(radius * f, color());
        }
        double radius() { return radius; }
    }

    static final class Rectangle extends AbstractShape {
        private final double width;
        private final double height;

        Rectangle(double width, double height, String color) {
            super(color);
            if (width <= 0 || height <= 0)
                throw new IllegalArgumentException("dimensions > 0 required");
            this.width  = width;
            this.height = height;
        }

        @Override public double area()      { return width * height; }
        @Override public double perimeter() { return 2 * (width + height); }
        @Override public String shapeName() { return "Rectangle"; }
        @Override protected AbstractShape scaleBy(double f) {
            return new Rectangle(width * f, height * f, color());
        }
        double width()  { return width;  }
        double height() { return height; }
        boolean isSquare() { return width == height; }
    }

    static final class Triangle extends AbstractShape {
        private final double a, b, c; // side lengths

        Triangle(double a, double b, double c, String color) {
            super(color);
            if (a <= 0 || b <= 0 || c <= 0)
                throw new IllegalArgumentException("sides must be > 0");
            if (a + b <= c || b + c <= a || a + c <= b)
                throw new IllegalArgumentException("invalid triangle sides");
            this.a = a; this.b = b; this.c = c;
        }

        @Override public double area() {
            // Heron's formula
            double s = (a + b + c) / 2;
            return Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }
        @Override public double perimeter() { return a + b + c; }
        @Override public String shapeName() { return "Triangle"; }
        @Override protected AbstractShape scaleBy(double f) {
            return new Triangle(a * f, b * f, c * f, color());
        }
    }

    static final class Ellipse extends AbstractShape {
        private final double semiMajor;
        private final double semiMinor;

        Ellipse(double semiMajor, double semiMinor, String color) {
            super(color);
            if (semiMajor <= 0 || semiMinor <= 0)
                throw new IllegalArgumentException("semi-axes > 0 required");
            this.semiMajor = semiMajor;
            this.semiMinor = semiMinor;
        }

        @Override public double area() { return Math.PI * semiMajor * semiMinor; }

        // Ramanujan approximation for ellipse perimeter
        @Override public double perimeter() {
            double h = Math.pow((semiMajor - semiMinor) / (semiMajor + semiMinor), 2);
            return Math.PI * (semiMajor + semiMinor) * (1 + 3 * h / (10 + Math.sqrt(4 - 3 * h)));
        }
        @Override public String shapeName() { return "Ellipse"; }
        @Override protected AbstractShape scaleBy(double f) {
            return new Ellipse(semiMajor * f, semiMinor * f, color());
        }
    }

    // -------------------------------------------------------------------------
    // 3. Polymorphic calculator
    // -------------------------------------------------------------------------
    static class Calculator {
        private final List<AbstractShape> shapes;

        Calculator(List<AbstractShape> shapes) {
            this.shapes = new ArrayList<>(shapes);
        }

        double totalArea() {
            return shapes.stream().mapToDouble(AbstractShape::area).sum();
        }

        double totalPerimeter() {
            return shapes.stream().mapToDouble(AbstractShape::perimeter).sum();
        }

        Optional<AbstractShape> largest() {
            return shapes.stream().min(Comparator.naturalOrder()); // descending order
        }

        Optional<AbstractShape> smallest() {
            return shapes.stream().max(Comparator.naturalOrder());
        }

        List<AbstractShape> sortedByAreaDesc() {
            return shapes.stream().sorted().toList();
        }

        Map<String, Long> countByType() {
            return shapes.stream()
                .collect(Collectors.groupingBy(AbstractShape::shapeName, Collectors.counting()));
        }

        Map<String, Double> totalAreaByType() {
            return shapes.stream()
                .collect(Collectors.groupingBy(
                    AbstractShape::shapeName,
                    Collectors.summingDouble(AbstractShape::area)));
        }

        List<AbstractShape> scaledAll(double factor) {
            return shapes.stream().map(s -> s.scaled(factor)).toList();
        }

        List<AbstractShape> filterByMinArea(double minArea) {
            return shapes.stream().filter(s -> s.area() >= minArea).toList();
        }

        void printReport() {
            System.out.println("  " + "─".repeat(65));
            sortedByAreaDesc().forEach(s -> System.out.println("  " + s));
            System.out.println("  " + "─".repeat(65));
            System.out.printf("  Total area:      %8.2f%n", totalArea());
            System.out.printf("  Total perimeter: %8.2f%n", totalPerimeter());
            largest() .ifPresent(s -> System.out.println("  Largest:  " + s.shapeName()
                + " area=" + String.format("%.2f", s.area())));
            smallest().ifPresent(s -> System.out.println("  Smallest: " + s.shapeName()
                + " area=" + String.format("%.2f", s.area())));

            System.out.println("\n  Count by type:");
            countByType().forEach((k, v) -> System.out.printf("    %-12s %d%n", k, v));

            System.out.println("\n  Area by type:");
            totalAreaByType().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(e -> System.out.printf("    %-12s %.2f%n", e.getKey(), e.getValue()));
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void abstractTemplateDemo() {
        System.out.println("=== Template Method (AbstractShape) ===");

        AbstractShape c = new Circle(5, "red");
        AbstractShape r = new Rectangle(4, 6, "blue");
        AbstractShape t = new Triangle(3, 4, 5, "green");
        AbstractShape e = new Ellipse(6, 4, "yellow");

        System.out.println(c.describe());
        System.out.println(r.describe());
        System.out.println(t.describe());
        System.out.println(e.describe());

        System.out.println("\nScaled by 2x:");
        System.out.println(c.scaled(2).describe());
        System.out.println(r.scaled(2).describe());
    }

    static void calculatorDemo() {
        System.out.println("\n=== ShapeCalculator (polymorphic pipeline) ===");

        List<AbstractShape> shapes = List.of(
            new Circle(7,    "red"),
            new Rectangle(8, 5, "blue"),
            new Triangle(6, 8, 10, "green"),
            new Ellipse(9, 4, "yellow"),
            new Circle(3,    "purple"),
            new Rectangle(4, 4, "orange"),
            new Triangle(5, 5, 5, "cyan")
        );

        Calculator calc = new Calculator(shapes);
        calc.printReport();

        System.out.println("\nFiltered (area >= 50):");
        calc.filterByMinArea(50).forEach(s ->
            System.out.printf("  %-12s area=%.2f%n", s.shapeName(), s.area()));
    }

    static void polymorphicDispatchDemo() {
        System.out.println("\n=== Polymorphic dispatch (instanceof pattern) ===");

        List<AbstractShape> mixed = List.of(
            new Circle(4, "red"),
            new Rectangle(3, 5, "blue"),
            new Rectangle(6, 6, "green"),   // square!
            new Triangle(3, 4, 5, "white")
        );

        for (AbstractShape s : mixed) {
            String extra = switch (s) {
                case Circle    c -> "radius=" + c.radius();
                case Rectangle r when r.isSquare() -> "SQUARE side=" + r.width();
                case Rectangle r -> "w=" + r.width() + " h=" + r.height();
                default          -> "";
            };
            System.out.println("  " + s.shapeName() + " " + extra);
        }
    }

    public static void main(String[] args) {
        abstractTemplateDemo();
        calculatorDemo();
        polymorphicDispatchDemo();
    }
}
