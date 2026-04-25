package com.javatraining.enums;

import java.util.*;

/**
 * TOPIC: EnumSet and EnumMap - specialized, high-performance collections
 * for enum types. Both are backed by simple data structures (bitset and array)
 * rather than hash tables, making them significantly faster than HashSet/HashMap.
 */
public class EnumCollections {

    enum Permission { READ, WRITE, DELETE, ADMIN, AUDIT }

    enum Weekday    { MON, TUE, WED, THU, FRI, SAT, SUN }

    enum Quarter    { Q1, Q2, Q3, Q4 }

    // -------------------------------------------------------------------------
    // EnumSet - backed by a single long bitmask for up to 64 constants
    // -------------------------------------------------------------------------
    static void enumSetDemo() {
        System.out.println("=== EnumSet ===");

        // Factory methods
        EnumSet<Permission> none      = EnumSet.noneOf(Permission.class);
        EnumSet<Permission> all       = EnumSet.allOf(Permission.class);
        EnumSet<Permission> readOnly  = EnumSet.of(Permission.READ);
        EnumSet<Permission> userPerms = EnumSet.of(Permission.READ, Permission.WRITE);
        EnumSet<Permission> adminFull = EnumSet.of(Permission.READ, Permission.WRITE,
                                                   Permission.DELETE, Permission.ADMIN);

        // Range - all constants between two endpoints (inclusive, by ordinal order)
        EnumSet<Permission> basic = EnumSet.range(Permission.READ, Permission.DELETE);
        // { READ, WRITE, DELETE }

        // Complement - everything NOT in the given set
        EnumSet<Permission> nonPrivileged = EnumSet.complementOf(
            EnumSet.of(Permission.DELETE, Permission.ADMIN)
        );
        // { READ, WRITE, AUDIT }

        System.out.println("none:          " + none);
        System.out.println("all:           " + all);
        System.out.println("readOnly:      " + readOnly);
        System.out.println("userPerms:     " + userPerms);
        System.out.println("basic (range): " + basic);
        System.out.println("nonPrivileged: " + nonPrivileged);

        // Membership operations - O(1) bitwise
        System.out.println("\ncontains READ:   " + userPerms.contains(Permission.READ));   // true
        System.out.println("contains DELETE: " + userPerms.contains(Permission.DELETE)); // false

        // Set operations
        EnumSet<Permission> combined = EnumSet.copyOf(userPerms);
        combined.addAll(EnumSet.of(Permission.AUDIT));
        System.out.println("combined:      " + combined); // READ, WRITE, AUDIT

        // Check if one set is a subset of another
        System.out.println("userPerms ⊆ adminFull: " + adminFull.containsAll(userPerms)); // true

        // Iteration is always in ordinal (declaration) order
        System.out.print("iteration order: ");
        for (Permission p : all) System.out.print(p + " ");
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Real use-case: role-based access control using EnumSet
    // -------------------------------------------------------------------------
    enum Role {
        VIEWER   (EnumSet.of(Permission.READ)),
        EDITOR   (EnumSet.of(Permission.READ, Permission.WRITE)),
        MANAGER  (EnumSet.of(Permission.READ, Permission.WRITE, Permission.DELETE)),
        ADMIN    (EnumSet.allOf(Permission.class));

        private final EnumSet<Permission> permissions;

        Role(EnumSet<Permission> permissions) {
            this.permissions = permissions;
        }

        public boolean can(Permission p) { return permissions.contains(p); }

        public EnumSet<Permission> permissions() {
            return EnumSet.copyOf(permissions); // defensive copy
        }
    }

    static void rbacDemo() {
        System.out.println("\n=== Role-Based Access Control ===");
        for (Role role : Role.values()) {
            System.out.printf("  %-8s → %s%n", role, role.permissions());
        }

        System.out.println("\nCan EDITOR delete?  " + Role.EDITOR.can(Permission.DELETE));  // false
        System.out.println("Can MANAGER delete? " + Role.MANAGER.can(Permission.DELETE)); // true
        System.out.println("Can VIEWER audit?   " + Role.VIEWER.can(Permission.AUDIT));   // false
        System.out.println("Can ADMIN audit?    " + Role.ADMIN.can(Permission.AUDIT));    // true
    }

    // -------------------------------------------------------------------------
    // EnumMap - backed by a plain array indexed by ordinal
    // -------------------------------------------------------------------------
    static void enumMapDemo() {
        System.out.println("\n=== EnumMap ===");

        // Mapping quarters to revenue
        EnumMap<Quarter, Double> revenue = new EnumMap<>(Quarter.class);
        revenue.put(Quarter.Q1, 450_000.0);
        revenue.put(Quarter.Q2, 520_000.0);
        revenue.put(Quarter.Q3, 610_000.0);
        revenue.put(Quarter.Q4, 780_000.0);

        // Iteration always in enum declaration order (Q1 → Q2 → Q3 → Q4)
        System.out.println("Quarterly Revenue:");
        double total = 0;
        for (Map.Entry<Quarter, Double> entry : revenue.entrySet()) {
            System.out.printf("  %s: ₹%,.0f%n", entry.getKey(), entry.getValue());
            total += entry.getValue();
        }
        System.out.printf("  Total: ₹%,.0f%n", total);

        // getOrDefault - useful for missing keys
        System.out.println("Q1 revenue: " + revenue.getOrDefault(Quarter.Q1, 0.0));

        // forEach - cleaner than entrySet iteration
        System.out.println("\nWeekday work hours:");
        EnumMap<Weekday, Integer> hours = new EnumMap<>(Weekday.class);
        hours.put(Weekday.MON, 8); hours.put(Weekday.TUE, 8);
        hours.put(Weekday.WED, 8); hours.put(Weekday.THU, 8);
        hours.put(Weekday.FRI, 6); hours.put(Weekday.SAT, 0);
        hours.put(Weekday.SUN, 0);
        hours.forEach((day, h) ->
            System.out.printf("  %s: %d hours%n", day, h));
    }

    // -------------------------------------------------------------------------
    // EnumMap: frequency counter
    // -------------------------------------------------------------------------
    static <E extends Enum<E>> EnumMap<E, Long> frequency(
            List<E> items, Class<E> type) {
        EnumMap<E, Long> counts = new EnumMap<>(type);
        for (E item : items) {
            counts.merge(item, 1L, Long::sum);
        }
        return counts;
    }

    // -------------------------------------------------------------------------
    // Performance comparison: EnumSet/EnumMap vs HashSet/HashMap
    // -------------------------------------------------------------------------
    static void performanceComparison() {
        System.out.println("\n=== Performance: EnumSet vs HashSet ===");
        int ITERATIONS = 5_000_000;

        // EnumSet
        EnumSet<Permission> enumSet = EnumSet.allOf(Permission.class);
        long start = System.nanoTime();
        boolean result = false;
        for (int i = 0; i < ITERATIONS; i++)
            result = enumSet.contains(Permission.DELETE);
        long enumSetTime = System.nanoTime() - start;

        // HashSet (equivalent)
        Set<Permission> hashSet = new HashSet<>(Arrays.asList(Permission.values()));
        start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++)
            result = hashSet.contains(Permission.DELETE);
        long hashSetTime = System.nanoTime() - start;

        System.out.printf("  EnumSet contains (%,d ops): %,d ms%n",
            ITERATIONS, enumSetTime  / 1_000_000);
        System.out.printf("  HashSet contains (%,d ops): %,d ms%n",
            ITERATIONS, hashSetTime / 1_000_000);
        System.out.printf("  EnumSet speedup: %.1fx%n",
            (double) hashSetTime / enumSetTime);
        System.out.println("  (result used to prevent JIT elimination: " + result + ")");
    }

    public static void main(String[] args) {
        enumSetDemo();
        rbacDemo();
        enumMapDemo();

        // Frequency counter demo
        System.out.println("\n=== EnumMap Frequency Counter ===");
        List<Weekday> schedule = List.of(
            Weekday.MON, Weekday.WED, Weekday.MON, Weekday.FRI,
            Weekday.WED, Weekday.MON, Weekday.SAT, Weekday.WED
        );
        EnumMap<Weekday, Long> freq = frequency(schedule, Weekday.class);
        freq.forEach((day, count) -> System.out.printf("  %s: %d meetings%n", day, count));

        performanceComparison();
    }
}
