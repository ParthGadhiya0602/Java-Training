package com.javatraining.oop;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TOPIC: Records (Java 16+) — immutable data carriers
 *
 * A record auto-generates:
 *   • private final fields for every component
 *   • canonical constructor (all components)
 *   • accessor methods (same name as field, no "get" prefix)
 *   • equals(), hashCode(), toString() — all derived from components
 *
 * What you CAN add to a record:
 *   • compact constructor (validation / normalisation — NO parameter list)
 *   • additional constructors (must delegate to canonical via this(...))
 *   • additional methods (pure functions on the data)
 *   • static fields and static methods
 *   • implement interfaces
 *
 * What you CANNOT do:
 *   • extend another class (records implicitly extend java.lang.Record)
 *   • declare instance fields outside the header
 *   • make a record mutable
 */
public class RecordsDemo {

    // -------------------------------------------------------------------------
    // 1. Minimal record — the compiler writes everything
    // -------------------------------------------------------------------------
    record Point(int x, int y) {
        // Nothing needed — equals, hashCode, toString, accessors all generated.
        // Accessor syntax: p.x(), p.y()  (NOT p.getX())

        // Additional method — records can have behaviour
        double distanceTo(Point other) {
            int dx = this.x - other.x;
            int dy = this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        // Static factory — expressive named constructor
        static Point origin() { return new Point(0, 0); }
    }

    // -------------------------------------------------------------------------
    // 2. Record with compact constructor — validation + normalisation
    //    Note: no parameter list on compact constructor
    // -------------------------------------------------------------------------
    record Range(int min, int max) {

        // Compact constructor: validate, then let the record assign fields
        Range {
            if (min > max) {
                // normalise: swap silently
                int tmp = min;
                min = max;
                max = tmp;
            }
        }

        int length()          { return max - min; }
        boolean contains(int v){ return v >= min && v <= max; }
        boolean overlaps(Range other) {
            return this.min <= other.max && other.min <= this.max;
        }

        // "with" pattern — records are immutable; create a modified copy
        Range withMin(int newMin) { return new Range(newMin, this.max); }
        Range withMax(int newMax) { return new Range(this.min, newMax); }
    }

    // -------------------------------------------------------------------------
    // 3. Record implementing an interface
    // -------------------------------------------------------------------------
    interface Shape {
        double area();
        double perimeter();
    }

    record Circle(double radius) implements Shape {
        Circle {
            if (radius <= 0) throw new IllegalArgumentException("radius must be > 0");
        }

        @Override public double area()      { return Math.PI * radius * radius; }
        @Override public double perimeter() { return 2 * Math.PI * radius; }
    }

    record Rectangle(double width, double height) implements Shape {
        Rectangle {
            if (width <= 0 || height <= 0)
                throw new IllegalArgumentException("dimensions must be > 0");
        }

        @Override public double area()      { return width * height; }
        @Override public double perimeter() { return 2 * (width + height); }
        boolean isSquare()                  { return width == height; }
    }

    // -------------------------------------------------------------------------
    // 4. Nested / composed records — records referencing other records
    // -------------------------------------------------------------------------
    record Address(String street, String city, String pincode) {
        Address {
            Objects.requireNonNull(street,  "street");
            Objects.requireNonNull(city,    "city");
            Objects.requireNonNull(pincode, "pincode");
            if (!pincode.matches("\\d{6}"))
                throw new IllegalArgumentException("pincode must be 6 digits: " + pincode);
        }
    }

    record Person(String name, int age, Address address) {
        Person {
            if (name.isBlank()) throw new IllegalArgumentException("name is blank");
            if (age < 0 || age > 150) throw new IllegalArgumentException("invalid age: " + age);
            Objects.requireNonNull(address, "address");
        }

        // Additional constructor — convenience, delegates to canonical
        Person(String name, int age) {
            this(name, age, new Address("Unknown", "Unknown", "000000"));
        }

        boolean isAdult() { return age >= 18; }

        // "with" copy helper
        Person withAge(int newAge) { return new Person(name, newAge, address); }
    }

    // -------------------------------------------------------------------------
    // 5. Record as DTO / value type in a collection pipeline
    // -------------------------------------------------------------------------
    record Product(String name, String category, double price, int stock) {
        Product {
            if (price < 0)  throw new IllegalArgumentException("negative price");
            if (stock < 0)  throw new IllegalArgumentException("negative stock");
        }

        boolean inStock()   { return stock > 0; }
        double totalValue() { return price * stock; }
    }

    // -------------------------------------------------------------------------
    // 6. Generic record (Java 16+)
    // -------------------------------------------------------------------------
    record Pair<A, B>(A first, B second) {
        static <X, Y> Pair<X, Y> of(X x, Y y) { return new Pair<>(x, y); }
        Pair<B, A> swapped() { return new Pair<>(second, first); }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void pointDemo() {
        System.out.println("=== Point record ===");
        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        Point p3 = Point.origin();

        System.out.println("p1:           " + p1);               // Point[x=3, y=4]
        System.out.println("p1.x():       " + p1.x());           // 3
        System.out.println("p1.equals(p2):" + p1.equals(p2));    // true — generated
        System.out.println("p1 == p2:     " + (p1 == p2));       // false — different objects
        System.out.println("distance:     " + p1.distanceTo(p3)); // 5.0
        System.out.println("origin:       " + p3);
    }

    static void rangeDemo() {
        System.out.println("\n=== Range (compact constructor — normalises order) ===");
        Range r1 = new Range(1, 10);
        Range r2 = new Range(10, 1);  // swapped — compact constructor normalises
        System.out.println("r1: " + r1 + " length=" + r1.length());
        System.out.println("r2: " + r2 + " (normalised from 10,1)");
        System.out.println("r1.contains(5):    " + r1.contains(5));
        System.out.println("r1.contains(11):   " + r1.contains(11));
        System.out.println("r1.overlaps(8,15): " + r1.overlaps(new Range(8, 15)));
        System.out.println("r1.withMax(20):    " + r1.withMax(20));
    }

    static void shapeDemo() {
        System.out.println("\n=== Shapes (records implementing interface) ===");
        List<Shape> shapes = List.of(
            new Circle(5),
            new Rectangle(4, 6),
            new Rectangle(3, 3),
            new Circle(1)
        );
        System.out.printf("  %-25s  %8s  %8s%n", "Shape", "Area", "Perimeter");
        System.out.println("  " + "─".repeat(45));
        for (Shape s : shapes) {
            String extra = (s instanceof Rectangle r && r.isSquare()) ? " [square]" : "";
            System.out.printf("  %-25s  %8.2f  %8.2f%s%n",
                s, s.area(), s.perimeter(), extra);
        }

        // Switch expression on record types (Java 21 pattern matching)
        System.out.println("\nDescriptions:");
        for (Shape s : shapes) {
            String desc = switch (s) {
                case Circle    c -> "circle r=" + c.radius();
                case Rectangle r -> "rect " + r.width() + "×" + r.height();
                default          -> "unknown";
            };
            System.out.println("  " + desc);
        }
    }

    static void personDemo() {
        System.out.println("\n=== Person with nested Address record ===");
        Address addr = new Address("123 MG Road", "Bengaluru", "560001");
        Person alice = new Person("Alice", 30, addr);
        Person minor = new Person("Bob", 15);  // convenience constructor

        System.out.println(alice);
        System.out.println("isAdult: " + alice.isAdult());
        System.out.println("city:    " + alice.address().city());

        // Immutable "update" via with-copy
        Person older = alice.withAge(31);
        System.out.println("Aged copy: " + older);
        System.out.println("Original unchanged: age=" + alice.age());

        // Validation
        try { new Address("x", "y", "12"); }
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void productPipelineDemo() {
        System.out.println("\n=== Product pipeline (records in streams) ===");
        List<Product> products = List.of(
            new Product("Laptop",    "Electronics", 75_000, 10),
            new Product("T-Shirt",   "Clothing",    1_200, 50),
            new Product("Novel",     "Books",         400, 0),
            new Product("Headphones","Electronics", 8_500, 25),
            new Product("Trousers",  "Clothing",    2_500, 15)
        );

        // Group by category, sum total value
        Map<String, Double> valueByCategory = products.stream()
            .filter(Product::inStock)
            .collect(Collectors.groupingBy(
                Product::category,
                Collectors.summingDouble(Product::totalValue)));

        System.out.println("Total inventory value by category:");
        valueByCategory.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(e -> System.out.printf("  %-14s ₹%,.0f%n", e.getKey(), e.getValue()));

        // Most expensive in-stock product
        products.stream()
            .filter(Product::inStock)
            .max(Comparator.comparingDouble(Product::price))
            .ifPresent(p -> System.out.println("Most expensive: " + p.name()
                + " @ ₹" + p.price()));
    }

    static void genericPairDemo() {
        System.out.println("\n=== Generic Pair record ===");
        Pair<String, Integer> nameAge = Pair.of("Alice", 30);
        System.out.println("nameAge:  " + nameAge);
        System.out.println("swapped:  " + nameAge.swapped());

        Pair<Integer, Integer> coord = Pair.of(10, 20);
        System.out.println("coord:    " + coord);
    }

    public static void main(String[] args) {
        pointDemo();
        rangeDemo();
        shapeDemo();
        personDemo();
        productPipelineDemo();
        genericPairDemo();
    }
}
