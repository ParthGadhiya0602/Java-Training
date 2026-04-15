package com.javatraining.oop;

import java.util.*;

/**
 * TOPIC: equals() / hashCode() contract
 *
 * The five rules every equals() MUST satisfy:
 *   1. Reflexive  — x.equals(x) == true
 *   2. Symmetric  — x.equals(y) ↔ y.equals(x)
 *   3. Transitive — x.equals(y) && y.equals(z) → x.equals(z)
 *   4. Consistent — same result every call, no side-effects
 *   5. Null-safe  — x.equals(null) == false (never throws NPE)
 *
 * The critical link:
 *   x.equals(y) → x.hashCode() == y.hashCode()
 *   (but equal hash does NOT imply equals — that's a hash collision)
 *
 * Violating this link silently breaks HashMap, HashSet, and any hash-based
 * collection — objects become "lost" even though they were inserted.
 */
public class EqualsHashCode {

    // -------------------------------------------------------------------------
    // 1. Correct implementation — field-by-field comparison
    // -------------------------------------------------------------------------
    static final class Point {
        private final int x;
        private final int y;

        Point(int x, int y) { this.x = x; this.y = y; }

        int x() { return x; }
        int y() { return y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;                  // reflexive fast path
            if (!(o instanceof Point other)) return false; // null-safe + type check
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            // Objects.hash() delegates to Arrays.hashCode — consistent, well-distributed
            return Objects.hash(x, y);
        }

        @Override
        public String toString() { return "Point(" + x + ", " + y + ")"; }
    }

    // -------------------------------------------------------------------------
    // 2. Broken hashCode — demonstrates the HashSet/HashMap failure mode
    //    DO NOT use this in production; it is intentionally wrong.
    // -------------------------------------------------------------------------
    static final class BrokenPoint {
        final int x, y;

        BrokenPoint(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BrokenPoint bp)) return false;
            return x == bp.x && y == bp.y;
        }

        // INTENTIONALLY BROKEN: always returns 0 — all points collide in one bucket.
        // While technically "consistent" with equals (equal objects have equal hash),
        // it turns every HashSet/HashMap into a linked-list: O(n) instead of O(1).
        @Override
        public int hashCode() { return 0; }

        @Override
        public String toString() { return "BrokenPoint(" + x + ", " + y + ")"; }
    }

    // -------------------------------------------------------------------------
    // 3. Symmetry trap — wrong equals when mixing subclasses
    //    This is why mixing class types in equals() is dangerous.
    // -------------------------------------------------------------------------
    static class ColorPoint {
        final int x, y;
        final String color;

        ColorPoint(int x, int y, String color) {
            this.x = x; this.y = y; this.color = color;
        }

        /**
         * Naive attempt: compare only coordinates when compared to a plain Point.
         * This looks convenient but BREAKS symmetry:
         *   point.equals(colorPoint) may be true  (Point ignores color)
         *   colorPoint.equals(point) is false (ColorPoint checks color)
         *
         * The correct fix: use getClass() or make Point sealed.
         * Here we intentionally show the wrong version to explain the trap.
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof ColorPoint cp) {
                return x == cp.x && y == cp.y && color.equals(cp.color);
            }
            if (o instanceof Point p) {
                return x == p.x() && y == p.y();  // ignores color — asymmetric!
            }
            return false;
        }

        @Override
        public int hashCode() { return Objects.hash(x, y, color); }

        @Override
        public String toString() { return "ColorPoint(" + x + "," + y + "," + color + ")"; }
    }

    // -------------------------------------------------------------------------
    // 4. Correct subclass strategy — use getClass() for strict type equality
    // -------------------------------------------------------------------------
    static class StrictPoint {
        final int x, y;

        StrictPoint(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            // getClass() instead of instanceof — rejects subclasses
            if (o == null || getClass() != o.getClass()) return false;
            StrictPoint sp = (StrictPoint) o;
            return x == sp.x && y == sp.y;
        }

        @Override
        public int hashCode() { return Objects.hash(x, y); }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void correctEqualsDemo() {
        System.out.println("=== Correct equals/hashCode (Point) ===");

        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        Point p3 = new Point(5, 6);

        // contract checks
        System.out.println("Reflexive:  p1.equals(p1) = " + p1.equals(p1));          // true
        System.out.println("Symmetric:  p1.equals(p2) = " + p1.equals(p2)
                         + "  p2.equals(p1) = " + p2.equals(p1));                    // both true
        System.out.println("Null-safe:  p1.equals(null) = " + p1.equals(null));       // false
        System.out.println("Different:  p1.equals(p3) = " + p1.equals(p3));           // false
        System.out.println("Hash equal: " + (p1.hashCode() == p2.hashCode()));         // true
        System.out.println("Hash diff:  " + (p1.hashCode() == p3.hashCode()));         // likely false

        // HashSet behaviour with correct implementation
        Set<Point> set = new HashSet<>();
        set.add(p1);
        System.out.println("\nHashSet contains new Point(3,4): "
            + set.contains(new Point(3, 4)));  // true — works correctly
        System.out.println("Set size after adding p1 then p2: "
            + (new HashSet<>(List.of(p1, p2))).size());  // 1 — deduplication works
    }

    static void brokenHashCodeDemo() {
        System.out.println("\n=== Broken hashCode consequences ===");

        Set<BrokenPoint> set = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            set.add(new BrokenPoint(i, i));
        }
        System.out.println("5 distinct points added to HashSet.");
        System.out.println("Contains (0,0): " + set.contains(new BrokenPoint(0, 0)));  // true (lucky)
        System.out.println("All points hash to: " + new BrokenPoint(99, 99).hashCode()); // always 0

        // Performance: all elements land in the same bucket → linear scan
        long start = System.nanoTime();
        Set<BrokenPoint> large = new HashSet<>();
        for (int i = 0; i < 1000; i++) large.add(new BrokenPoint(i, i));
        large.contains(new BrokenPoint(999, 999));
        long brokenTime = System.nanoTime() - start;

        start = System.nanoTime();
        Set<Point> good = new HashSet<>();
        for (int i = 0; i < 1000; i++) good.add(new Point(i, i));
        good.contains(new Point(999, 999));
        long goodTime = System.nanoTime() - start;

        System.out.printf("Lookup in 1000-element HashSet:%n");
        System.out.printf("  Broken hashCode: %,d ns%n", brokenTime);
        System.out.printf("  Correct hashCode: %,d ns%n", goodTime);
    }

    static void symmetryTrapDemo() {
        System.out.println("\n=== Symmetry trap (ColorPoint) ===");

        Point      p  = new Point(1, 2);
        ColorPoint cp = new ColorPoint(1, 2, "RED");

        System.out.println("p.equals(cp):  " + p.equals(cp));   // false (Point uses instanceof)
        System.out.println("cp.equals(p):  " + cp.equals(p));   // true  (ColorPoint accepts Point)
        System.out.println("Symmetric?     " + (p.equals(cp) == cp.equals(p))); // FALSE — broken!

        // HashMap consequence
        Map<ColorPoint, String> map = new HashMap<>();
        map.put(cp, "value");
        // Lookup using coordinates only — may not find it due to different hashCode
        System.out.println("map.get(cp) = " + map.get(cp));   // "value"
        System.out.println("map.get plain Point: hash mismatch → likely null");
    }

    static void hashMapDemo() {
        System.out.println("\n=== HashMap key lookup requires correct equals+hashCode ===");

        Map<Point, String> capitals = new HashMap<>();
        capitals.put(new Point(0, 0), "origin");
        capitals.put(new Point(1, 0), "east");
        capitals.put(new Point(0, 1), "north");

        // Lookup with a NEW object that is equal — works because equals+hashCode is correct
        System.out.println("Lookup (0,0): " + capitals.get(new Point(0, 0)));  // "origin"
        System.out.println("Lookup (1,0): " + capitals.get(new Point(1, 0)));  // "east"
        System.out.println("Lookup (9,9): " + capitals.get(new Point(9, 9)));  // null
    }

    public static void main(String[] args) {
        correctEqualsDemo();
        brokenHashCodeDemo();
        symmetryTrapDemo();
        hashMapDemo();
    }
}
