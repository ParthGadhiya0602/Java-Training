package com.javatraining.encapsulation;

import java.time.LocalDate;
import java.util.*;

/**
 * TOPIC: Immutability - how to build truly immutable classes
 *
 * An immutable object's state never changes after construction.
 * Benefits:
 *   • Thread-safe without synchronisation
 *   • Safe to share and cache
 *   • Eliminates temporal coupling and aliasing bugs
 *   • Easier to reason about - no "what state is this in?" questions
 *
 * The immutability checklist:
 *   1. All fields private final
 *   2. No setters
 *   3. Class final (blocks subclass mutation)
 *   4. Defensive copy mutable args IN  (constructor)
 *   5. Defensive copy mutable fields OUT (getters)
 *   6. Never return references to internal mutable objects
 */
public class ImmutableTypes {

    // -------------------------------------------------------------------------
    // 1. Unsafe mutable class - demonstrates aliasing / escaping reference bugs
    //    DO NOT use this style in production; shown to explain WHY we need defence.
    // -------------------------------------------------------------------------
    static class UnsafeSchedule {
        private final String name;
        private final List<LocalDate> slots; // ← UNSAFE: stores external reference

        UnsafeSchedule(String name, List<LocalDate> slots) {
            this.name  = name;
            this.slots = slots; // aliased - caller still holds the original list!
        }

        List<LocalDate> slots() { return slots; } // exposes internal mutable state
    }

    // -------------------------------------------------------------------------
    // 2. Safe immutable version of the same class
    // -------------------------------------------------------------------------
    static final class ImmutableSchedule {
        private final String          name;
        private final List<LocalDate> slots; // always an unmodifiable copy

        ImmutableSchedule(String name, List<LocalDate> slots) {
            this.name  = Objects.requireNonNull(name, "name");
            // Defensive copy IN - detach from caller's list, wrap unmodifiable
            this.slots = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(slots, "slots")));
        }

        String name() { return name; }

        // Defensive copy OUT - return a new list; caller cannot affect internals
        List<LocalDate> slots() { return new ArrayList<>(slots); }

        // "with" pattern - create a modified copy without mutation
        ImmutableSchedule withName(String newName) {
            return new ImmutableSchedule(newName, slots);
        }

        ImmutableSchedule withSlot(LocalDate slot) {
            List<LocalDate> updated = new ArrayList<>(slots);
            updated.add(slot);
            return new ImmutableSchedule(name, updated);
        }

        ImmutableSchedule withoutSlot(LocalDate slot) {
            List<LocalDate> updated = new ArrayList<>(slots);
            updated.remove(slot);
            return new ImmutableSchedule(name, updated);
        }

        int slotCount() { return slots.size(); }

        @Override public String toString() { return "Schedule{" + name + ", " + slots + "}"; }
    }

    // -------------------------------------------------------------------------
    // 3. Immutable Money - canonical example with arithmetic
    //    Uses long paise to avoid floating-point imprecision.
    // -------------------------------------------------------------------------
    static final class Money {
        private final long   paise;    // 1 rupee = 100 paise
        private final String currency;

        private Money(long paise, String currency) {
            if (paise < 0)
                throw new IllegalArgumentException("Money cannot be negative");
            this.paise    = paise;
            this.currency = Objects.requireNonNull(currency, "currency");
        }

        static Money of(double rupees, String currency) {
            return new Money(Math.round(rupees * 100), currency);
        }

        static Money zero(String currency) { return new Money(0, currency); }

        Money add(Money other) {
            checkCurrency(other);
            return new Money(paise + other.paise, currency);
        }

        Money subtract(Money other) {
            checkCurrency(other);
            if (paise < other.paise)
                throw new ArithmeticException("Would result in negative money");
            return new Money(paise - other.paise, currency);
        }

        Money multiply(double factor) {
            if (factor < 0) throw new IllegalArgumentException("factor must be >= 0");
            return new Money(Math.round(paise * factor), currency);
        }

        Money percentage(double pct) { return multiply(pct / 100.0); }

        boolean isGreaterThan(Money other) { checkCurrency(other); return paise > other.paise; }
        boolean isZero()                   { return paise == 0; }
        double  toRupees()                 { return paise / 100.0; }
        String  currency()                 { return currency; }
        long    toPaise()                  { return paise; }

        private void checkCurrency(Money other) {
            if (!currency.equals(other.currency))
                throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Money m)) return false;
            return paise == m.paise && currency.equals(m.currency);
        }

        @Override public int hashCode() { return Objects.hash(paise, currency); }

        @Override
        public String toString() {
            return String.format("%s %.2f", currency, toRupees());
        }
    }

    // -------------------------------------------------------------------------
    // 4. Immutable complex object - Period with mutable Date fields
    //    Dates are mutable; must copy both in AND out.
    // -------------------------------------------------------------------------
    static final class DateRange {
        private final LocalDate start; // LocalDate is immutable - no copy needed
        private final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end,   "end");
            if (end.isBefore(start))
                throw new IllegalArgumentException("end must not be before start");
            this.start = start;
            this.end   = end;
        }

        // LocalDate is already immutable - safe to return directly
        LocalDate start() { return start; }
        LocalDate end()   { return end;   }

        long days() { return start.until(end, java.time.temporal.ChronoUnit.DAYS); }

        boolean contains(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }

        boolean overlaps(DateRange other) {
            return !end.isBefore(other.start) && !other.end.isBefore(start);
        }

        DateRange extendBy(long days) {
            return new DateRange(start, end.plusDays(days));
        }

        DateRange shiftBy(long days) {
            return new DateRange(start.plusDays(days), end.plusDays(days));
        }

        @Override
        public String toString() { return "[" + start + " → " + end + "]"; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof DateRange d)) return false;
            return start.equals(d.start) && end.equals(d.end);
        }

        @Override public int hashCode() { return Objects.hash(start, end); }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void aliasingBugDemo() {
        System.out.println("=== Aliasing Bug (UnsafeSchedule) ===");

        List<LocalDate> slots = new ArrayList<>();
        slots.add(LocalDate.of(2024, 1, 10));
        slots.add(LocalDate.of(2024, 1, 15));

        UnsafeSchedule unsafe = new UnsafeSchedule("Doctor", slots);
        System.out.println("Before: " + unsafe.slots().size() + " slots");

        // Caller mutates the original list - schedule is silently corrupted!
        slots.add(LocalDate.of(2024, 1, 20));
        System.out.println("After adding to external list: " + unsafe.slots().size() + " slots");

        // Caller mutates via the getter - also corrupted!
        unsafe.slots().add(LocalDate.of(2024, 2, 1));
        System.out.println("After mutating via getter:     " + unsafe.slots().size() + " slots");
    }

    static void immutableScheduleDemo() {
        System.out.println("\n=== ImmutableSchedule (defensive copies) ===");

        List<LocalDate> slots = new ArrayList<>();
        slots.add(LocalDate.of(2024, 1, 10));
        slots.add(LocalDate.of(2024, 1, 15));

        ImmutableSchedule s1 = new ImmutableSchedule("Doctor", slots);
        System.out.println("Original: " + s1.slotCount() + " slots");

        // Mutation of source list doesn't affect the immutable object
        slots.add(LocalDate.of(2024, 1, 20));
        System.out.println("After mutating source list: " + s1.slotCount() + " slots (unchanged)");

        // Mutation of returned list doesn't affect internals
        List<LocalDate> returned = s1.slots();
        returned.add(LocalDate.of(2024, 3, 1));
        System.out.println("After mutating returned list: " + s1.slotCount() + " slots (unchanged)");

        // "with" pattern - non-destructive update
        ImmutableSchedule s2 = s1.withSlot(LocalDate.of(2024, 2, 5));
        System.out.println("s1 slots: " + s1.slotCount() + "  s2 slots: " + s2.slotCount());

        ImmutableSchedule s3 = s2.withName("Dentist");
        System.out.println("s3: " + s3.name() + " (" + s3.slotCount() + " slots)");
    }

    static void moneyDemo() {
        System.out.println("\n=== Immutable Money (paise-based) ===");

        Money salary   = Money.of(50_000, "INR");
        Money bonus    = Money.of(10_000, "INR");
        Money tax      = Money.of(6_000,  "INR");
        Money hra      = salary.percentage(40);

        System.out.println("Salary:    " + salary);
        System.out.println("Bonus:     " + bonus);
        System.out.println("HRA (40%): " + hra);
        System.out.println("Gross:     " + salary.add(bonus));
        System.out.println("Net:       " + salary.add(bonus).subtract(tax));
        System.out.println("Bonus > Tax: " + bonus.isGreaterThan(tax));

        // Immutability: original unchanged after arithmetic
        Money doubled = salary.multiply(2);
        System.out.println("Original salary unchanged: " + salary);
        System.out.println("Doubled: " + doubled);

        try { salary.add(Money.of(100, "USD")); }
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void dateRangeDemo() {
        System.out.println("\n=== DateRange (immutable with LocalDate) ===");

        DateRange q1 = new DateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));
        DateRange q2 = new DateRange(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 6, 30));

        System.out.println("Q1: " + q1 + " days=" + q1.days());
        System.out.println("Q2: " + q2);
        System.out.println("Q1 contains Feb 15: " + q1.contains(LocalDate.of(2024, 2, 15)));
        System.out.println("Q1 overlaps Q2: " + q1.overlaps(q2));

        DateRange extended = q1.extendBy(10);
        System.out.println("Q1 extended by 10: " + extended);
        System.out.println("Original Q1 unchanged: " + q1);

        try { new DateRange(LocalDate.of(2024, 12, 31), LocalDate.of(2024, 1, 1)); }
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        aliasingBugDemo();
        immutableScheduleDemo();
        moneyDemo();
        dateRangeDemo();
    }
}
