package com.javatraining.encapsulation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImmutableTypesTest {

    // -----------------------------------------------------------------------
    // ImmutableSchedule - defensive copy in & out
    // -----------------------------------------------------------------------
    @Test
    void mutating_source_list_does_not_affect_schedule() {
        List<LocalDate> slots = new ArrayList<>();
        slots.add(LocalDate.of(2024, 1, 10));
        ImmutableTypes.ImmutableSchedule s = new ImmutableTypes.ImmutableSchedule("Test", slots);

        slots.add(LocalDate.of(2024, 2, 1));   // mutate source after construction
        assertEquals(1, s.slotCount());         // schedule must be unchanged
    }

    @Test
    void mutating_returned_list_does_not_affect_schedule() {
        ImmutableTypes.ImmutableSchedule s = new ImmutableTypes.ImmutableSchedule(
            "Test", List.of(LocalDate.of(2024, 1, 10)));

        List<LocalDate> returned = s.slots();
        returned.add(LocalDate.of(2024, 3, 1)); // mutate returned list
        assertEquals(1, s.slotCount());          // schedule must be unchanged
    }

    @Test
    void with_slot_returns_new_instance_with_added_slot() {
        ImmutableTypes.ImmutableSchedule s1 = new ImmutableTypes.ImmutableSchedule(
            "Test", List.of(LocalDate.of(2024, 1, 10)));
        ImmutableTypes.ImmutableSchedule s2 = s1.withSlot(LocalDate.of(2024, 2, 5));

        assertEquals(1, s1.slotCount());   // original unchanged
        assertEquals(2, s2.slotCount());   // new instance has the extra slot
    }

    @Test
    void with_name_returns_new_instance_same_slots() {
        ImmutableTypes.ImmutableSchedule s1 = new ImmutableTypes.ImmutableSchedule(
            "Doctor", List.of(LocalDate.of(2024, 1, 10)));
        ImmutableTypes.ImmutableSchedule s2 = s1.withName("Dentist");

        assertEquals("Doctor", s1.name());
        assertEquals("Dentist", s2.name());
        assertEquals(s1.slotCount(), s2.slotCount());
    }

    @Test
    void null_name_throws() {
        assertThrows(NullPointerException.class, () ->
            new ImmutableTypes.ImmutableSchedule(null, List.of()));
    }

    // -----------------------------------------------------------------------
    // Money - immutable arithmetic
    // -----------------------------------------------------------------------
    @Test
    void add_creates_new_instance_originals_unchanged() {
        ImmutableTypes.Money a = ImmutableTypes.Money.of(1_000, "INR");
        ImmutableTypes.Money b = ImmutableTypes.Money.of(500,   "INR");
        ImmutableTypes.Money c = a.add(b);
        assertEquals(1_000.0, a.toRupees(), 1e-9);  // unchanged
        assertEquals(1_500.0, c.toRupees(), 1e-9);
    }

    @Test
    void subtract_below_zero_throws() {
        ImmutableTypes.Money a = ImmutableTypes.Money.of(100, "INR");
        ImmutableTypes.Money b = ImmutableTypes.Money.of(200, "INR");
        assertThrows(ArithmeticException.class, () -> a.subtract(b));
    }

    @Test
    void multiply_by_factor() {
        ImmutableTypes.Money m = ImmutableTypes.Money.of(1_000, "INR");
        assertEquals(1_500.0, m.multiply(1.5).toRupees(), 0.01);
    }

    @Test
    void percentage_of_amount() {
        ImmutableTypes.Money m = ImmutableTypes.Money.of(10_000, "INR");
        assertEquals(1_800.0, m.percentage(18).toRupees(), 0.01);  // 18% GST
    }

    @Test
    void negative_factor_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            ImmutableTypes.Money.of(100, "INR").multiply(-1));
    }

    @Test
    void currency_mismatch_throws() {
        ImmutableTypes.Money inr = ImmutableTypes.Money.of(100, "INR");
        ImmutableTypes.Money usd = ImmutableTypes.Money.of(100, "USD");
        assertThrows(IllegalArgumentException.class, () -> inr.add(usd));
    }

    @Test
    void zero_factory() {
        ImmutableTypes.Money z = ImmutableTypes.Money.zero("INR");
        assertTrue(z.isZero());
        assertEquals(0.0, z.toRupees(), 1e-9);
    }

    @Test
    void equals_and_hashcode() {
        ImmutableTypes.Money a = ImmutableTypes.Money.of(500, "INR");
        ImmutableTypes.Money b = ImmutableTypes.Money.of(500, "INR");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // -----------------------------------------------------------------------
    // DateRange
    // -----------------------------------------------------------------------
    @Test
    void date_range_days_count() {
        ImmutableTypes.DateRange r = new ImmutableTypes.DateRange(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        assertEquals(30, r.days());
    }

    @Test
    void contains_date() {
        ImmutableTypes.DateRange r = new ImmutableTypes.DateRange(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));
        assertTrue(r.contains(LocalDate.of(2024, 2, 15)));
        assertFalse(r.contains(LocalDate.of(2023, 12, 31)));
        assertFalse(r.contains(LocalDate.of(2024, 4, 1)));
    }

    @Test
    void overlaps() {
        ImmutableTypes.DateRange q1 = new ImmutableTypes.DateRange(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31));
        ImmutableTypes.DateRange q2 = new ImmutableTypes.DateRange(
            LocalDate.of(2024, 4, 1), LocalDate.of(2024, 6, 30));
        ImmutableTypes.DateRange partial = new ImmutableTypes.DateRange(
            LocalDate.of(2024, 3, 15), LocalDate.of(2024, 5, 1));

        assertFalse(q1.overlaps(q2));
        assertTrue(q1.overlaps(partial));
    }

    @Test
    void extend_by_creates_new_instance() {
        ImmutableTypes.DateRange r = new ImmutableTypes.DateRange(
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        ImmutableTypes.DateRange extended = r.extendBy(10);
        assertEquals(LocalDate.of(2024, 1, 31), r.end());      // unchanged
        assertEquals(LocalDate.of(2024, 2, 10), extended.end());
    }

    @Test
    void end_before_start_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new ImmutableTypes.DateRange(
                LocalDate.of(2024, 12, 31), LocalDate.of(2024, 1, 1)));
    }
}
