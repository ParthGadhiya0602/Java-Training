package com.javatraining.oop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RecordsDemoTest {

    // -----------------------------------------------------------------------
    // Point record
    // -----------------------------------------------------------------------
    @Test
    void point_accessors() {
        RecordsDemo.Point p = new RecordsDemo.Point(3, 4);
        assertEquals(3, p.x());
        assertEquals(4, p.y());
    }

    @Test
    void point_equals_and_hashcode_generated() {
        RecordsDemo.Point a = new RecordsDemo.Point(1, 2);
        RecordsDemo.Point b = new RecordsDemo.Point(1, 2);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void point_distance_to_origin() {
        RecordsDemo.Point p = new RecordsDemo.Point(3, 4);
        assertEquals(5.0, p.distanceTo(RecordsDemo.Point.origin()), 1e-9);
    }

    @Test
    void point_origin_is_zero_zero() {
        RecordsDemo.Point o = RecordsDemo.Point.origin();
        assertEquals(0, o.x());
        assertEquals(0, o.y());
    }

    @Test
    void point_toString_record_format() {
        // Records produce: ClassName[field1=v1, field2=v2]
        assertEquals("Point[x=1, y=2]", new RecordsDemo.Point(1, 2).toString());
    }

    // -----------------------------------------------------------------------
    // Range — compact constructor normalises order
    // -----------------------------------------------------------------------
    @Test
    void range_normal_order() {
        RecordsDemo.Range r = new RecordsDemo.Range(1, 10);
        assertEquals(1,  r.min());
        assertEquals(10, r.max());
    }

    @Test
    void range_reversed_order_normalised() {
        RecordsDemo.Range r = new RecordsDemo.Range(10, 1);
        assertEquals(1,  r.min());
        assertEquals(10, r.max());
    }

    @Test
    void range_length() {
        assertEquals(9, new RecordsDemo.Range(1, 10).length());
    }

    @ParameterizedTest
    @CsvSource({"5, true", "1, true", "10, true", "0, false", "11, false"})
    void range_contains(int value, boolean expected) {
        RecordsDemo.Range r = new RecordsDemo.Range(1, 10);
        assertEquals(expected, r.contains(value));
    }

    @Test
    void range_overlaps() {
        RecordsDemo.Range r = new RecordsDemo.Range(1, 10);
        assertTrue(r.overlaps(new RecordsDemo.Range(5, 15)));
        assertFalse(r.overlaps(new RecordsDemo.Range(11, 20)));
    }

    @Test
    void range_with_copy_does_not_mutate_original() {
        RecordsDemo.Range original = new RecordsDemo.Range(1, 10);
        RecordsDemo.Range copy = original.withMax(20);
        assertEquals(10, original.max()); // unchanged
        assertEquals(20, copy.max());
    }

    // -----------------------------------------------------------------------
    // Circle — validation in compact constructor
    // -----------------------------------------------------------------------
    @Test
    void circle_area_and_perimeter() {
        RecordsDemo.Circle c = new RecordsDemo.Circle(5);
        assertEquals(Math.PI * 25, c.area(),      1e-9);
        assertEquals(Math.PI * 10, c.perimeter(), 1e-9);
    }

    @Test
    void circle_zero_radius_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RecordsDemo.Circle(0));
    }

    @Test
    void circle_negative_radius_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RecordsDemo.Circle(-1));
    }

    // -----------------------------------------------------------------------
    // Rectangle
    // -----------------------------------------------------------------------
    @Test
    void rectangle_area_and_perimeter() {
        RecordsDemo.Rectangle r = new RecordsDemo.Rectangle(4, 6);
        assertEquals(24.0, r.area(),      1e-9);
        assertEquals(20.0, r.perimeter(), 1e-9);
    }

    @Test
    void rectangle_is_square_when_equal_sides() {
        assertTrue(new RecordsDemo.Rectangle(3, 3).isSquare());
        assertFalse(new RecordsDemo.Rectangle(3, 4).isSquare());
    }

    // -----------------------------------------------------------------------
    // Address — pincode validation
    // -----------------------------------------------------------------------
    @Test
    void address_valid() {
        assertDoesNotThrow(() -> new RecordsDemo.Address("123 MG Road", "Bengaluru", "560001"));
    }

    @Test
    void address_invalid_pincode_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new RecordsDemo.Address("x", "y", "12345"));   // only 5 digits
        assertThrows(IllegalArgumentException.class,
            () -> new RecordsDemo.Address("x", "y", "ABCDEF"));  // non-numeric
    }

    // -----------------------------------------------------------------------
    // Person
    // -----------------------------------------------------------------------
    @Test
    void person_is_adult() {
        RecordsDemo.Address addr = new RecordsDemo.Address("A", "B", "123456");
        assertTrue(new RecordsDemo.Person("Alice", 18, addr).isAdult());
        assertFalse(new RecordsDemo.Person("Bob",  17, addr).isAdult());
    }

    @Test
    void person_with_age_returns_copy_original_unchanged() {
        RecordsDemo.Address addr = new RecordsDemo.Address("A", "B", "123456");
        RecordsDemo.Person alice = new RecordsDemo.Person("Alice", 30, addr);
        RecordsDemo.Person older = alice.withAge(31);
        assertEquals(30, alice.age()); // original unchanged
        assertEquals(31, older.age());
        assertEquals("Alice", older.name());
    }

    // -----------------------------------------------------------------------
    // Generic Pair
    // -----------------------------------------------------------------------
    @Test
    void pair_accessors() {
        RecordsDemo.Pair<String, Integer> p = RecordsDemo.Pair.of("hello", 42);
        assertEquals("hello", p.first());
        assertEquals(42,      p.second());
    }

    @Test
    void pair_swapped() {
        RecordsDemo.Pair<Integer, String> swapped =
            RecordsDemo.Pair.of("x", 1).swapped();
        assertEquals(1,   swapped.first());
        assertEquals("x", swapped.second());
    }
}
