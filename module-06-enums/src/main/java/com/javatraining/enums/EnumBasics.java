package com.javatraining.enums;

import java.util.Arrays;

/**
 * TOPIC: Enum fundamentals — declaration, built-in methods, fields,
 * constructors, method overriding, and reverse lookup.
 */
public class EnumBasics {

    // -------------------------------------------------------------------------
    // 1. Simple enum — no fields, no methods
    // -------------------------------------------------------------------------
    enum Direction { NORTH, SOUTH, EAST, WEST }

    // -------------------------------------------------------------------------
    // 2. Enum with fields, constructor, methods, and overridden toString()
    // -------------------------------------------------------------------------
    enum HttpStatus {
        OK             (200, "OK"),
        CREATED        (201, "Created"),
        NO_CONTENT     (204, "No Content"),
        BAD_REQUEST    (400, "Bad Request"),
        UNAUTHORIZED   (401, "Unauthorized"),
        FORBIDDEN      (403, "Forbidden"),
        NOT_FOUND      (404, "Not Found"),
        CONFLICT       (409, "Conflict"),
        INTERNAL_ERROR (500, "Internal Server Error"),
        BAD_GATEWAY    (502, "Bad Gateway"),
        UNAVAILABLE    (503, "Service Unavailable");

        private final int    code;
        private final String reason;

        // Constructor is always private (implicitly)
        HttpStatus(int code, String reason) {
            this.code   = code;
            this.reason = reason;
        }

        public int    code()   { return code; }
        public String reason() { return reason; }

        public boolean isSuccess()    { return code >= 200 && code < 300; }
        public boolean isClientError(){ return code >= 400 && code < 500; }
        public boolean isServerError(){ return code >= 500; }

        @Override
        public String toString() { return code + " " + reason; }

        // Reverse lookup by numeric code — common production pattern
        public static HttpStatus fromCode(int code) {
            for (HttpStatus s : values()) {
                if (s.code == code) return s;
            }
            throw new IllegalArgumentException("Unknown HTTP status code: " + code);
        }
    }

    // -------------------------------------------------------------------------
    // 3. Planet enum — computed properties based on fields
    // -------------------------------------------------------------------------
    enum Planet {
        MERCURY (3.303e+23, 2.4397e6),
        VENUS   (4.869e+24, 6.0518e6),
        EARTH   (5.976e+24, 6.37814e6),
        MARS    (6.421e+23, 3.3972e6),
        JUPITER (1.899e+27, 7.1492e7),
        SATURN  (5.685e+26, 6.0268e7),
        URANUS  (8.683e+25, 2.5559e7),
        NEPTUNE (1.024e+26, 2.4746e7);

        private static final double G = 6.67300E-11; // gravitational constant

        private final double mass;    // kg
        private final double radius;  // metres

        Planet(double mass, double radius) {
            this.mass   = mass;
            this.radius = radius;
        }

        public double surfaceGravity() {
            return G * mass / (radius * radius);
        }

        public double surfaceWeight(double bodyMassKg) {
            return bodyMassKg * surfaceGravity();
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void builtInMethods() {
        Direction dir = Direction.NORTH;

        System.out.println("name():    " + dir.name());      // "NORTH"
        System.out.println("ordinal(): " + dir.ordinal());   // 0
        System.out.println("toString():" + dir.toString());  // "NORTH"

        // == is safe and correct for enums (each constant is a singleton)
        System.out.println("== check:  " + (dir == Direction.NORTH)); // true

        // values() — returns a new array of all constants in declaration order
        System.out.println("All directions: " + Arrays.toString(Direction.values()));

        // valueOf() — get constant by name string (case-sensitive)
        Direction east = Direction.valueOf("EAST");
        System.out.println("valueOf EAST: " + east);

        // compareTo compares by ordinal
        System.out.println("NORTH.compareTo(SOUTH): "
            + Direction.NORTH.compareTo(Direction.SOUTH)); // negative (0 < 1)

        // Ordinal warning: fragile — changes if you insert a new constant
        System.out.println("SOUTH ordinal: " + Direction.SOUTH.ordinal()); // 1
    }

    static void httpStatusDemo() {
        System.out.println("\n=== HTTP Status ===");
        HttpStatus status = HttpStatus.NOT_FOUND;

        System.out.println("Status:    " + status);             // 404 Not Found
        System.out.println("Code:      " + status.code());      // 404
        System.out.println("Reason:    " + status.reason());    // "Not Found"
        System.out.println("isClientError: " + status.isClientError()); // true
        System.out.println("isServerError: " + status.isServerError()); // false

        // Reverse lookup
        System.out.println("fromCode(200): " + HttpStatus.fromCode(200)); // 200 OK
        System.out.println("fromCode(500): " + HttpStatus.fromCode(500)); // 500 Internal...

        // Group by category
        System.out.println("\nServer errors:");
        for (HttpStatus s : HttpStatus.values()) {
            if (s.isServerError())
                System.out.println("  " + s);
        }

        try {
            HttpStatus.fromCode(999);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    static void planetDemo() {
        System.out.println("\n=== Surface weight on each planet (75 kg person) ===");
        double bodyMass = 75.0 / Planet.EARTH.surfaceGravity(); // mass in kg
        for (Planet p : Planet.values()) {
            System.out.printf("  %-8s: %6.2f N%n", p, p.surfaceWeight(bodyMass));
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Built-in Enum Methods ===");
        builtInMethods();
        httpStatusDemo();
        planetDemo();
    }
}
