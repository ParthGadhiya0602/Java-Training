package com.javatraining.interfaces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class InterfaceFeaturesTest {

    // -----------------------------------------------------------------------
    // Validator — static factories
    // -----------------------------------------------------------------------
    @Test
    void nonBlank_rejects_null_and_blank() {
        InterfaceFeatures.Validator<String> v = InterfaceFeatures.Validator.nonBlank();
        assertFalse(v.isValid(null));
        assertFalse(v.isValid(""));
        assertFalse(v.isValid("  "));
        assertTrue(v.isValid("hello"));
    }

    @ParameterizedTest
    @CsvSource({ "hello, true", "hi, false", "hey, false" })
    void minLength5(String s, boolean expected) {
        assertEquals(expected, InterfaceFeatures.Validator.minLength(5).isValid(s));
    }

    @ParameterizedTest
    @CsvSource({ "abc, true", "abcdefghij, false" })
    void maxLength5(String s, boolean expected) {
        assertEquals(expected, InterfaceFeatures.Validator.maxLength(5).isValid(s));
    }

    @Test
    void positive_validator() {
        InterfaceFeatures.Validator<Integer> v = InterfaceFeatures.Validator.positive();
        assertTrue(v.isValid(1));
        assertFalse(v.isValid(0));
        assertFalse(v.isValid(-5));
    }

    @Test
    void range_validator() {
        InterfaceFeatures.Validator<Integer> v = InterfaceFeatures.Validator.range(1, 10);
        assertTrue(v.isValid(1));
        assertTrue(v.isValid(10));
        assertFalse(v.isValid(0));
        assertFalse(v.isValid(11));
    }

    // -----------------------------------------------------------------------
    // Validator — default composition
    // -----------------------------------------------------------------------
    @Test
    void and_both_must_pass() {
        InterfaceFeatures.Validator<String> v =
            InterfaceFeatures.Validator.minLength(3)
                .and(InterfaceFeatures.Validator.maxLength(6));
        assertTrue(v.isValid("abc"));
        assertFalse(v.isValid("ab"));
        assertFalse(v.isValid("abcdefg"));
    }

    @Test
    void or_either_passes() {
        InterfaceFeatures.Validator<Integer> v =
            InterfaceFeatures.Validator.<Integer>positive()
                .or(n -> n == -1);
        assertTrue(v.isValid(5));
        assertTrue(v.isValid(-1));
        assertFalse(v.isValid(-2));
    }

    @Test
    void negate_flips_result() {
        InterfaceFeatures.Validator<Integer> notPositive =
            InterfaceFeatures.Validator.<Integer>positive().negate();
        assertTrue(notPositive.isValid(-3));
        assertFalse(notPositive.isValid(3));
    }

    @Test
    void validated_returns_value_when_valid() {
        InterfaceFeatures.Validator<String> v = InterfaceFeatures.Validator.nonBlank();
        assertEquals("hello", v.validated("hello"));
    }

    @Test
    void validated_throws_when_invalid() {
        InterfaceFeatures.Validator<String> v = InterfaceFeatures.Validator.nonBlank();
        assertThrows(IllegalArgumentException.class, () -> v.validated(""));
    }

    // -----------------------------------------------------------------------
    // Diamond default resolution
    // -----------------------------------------------------------------------
    @Test
    void middle_greet_contains_both_sides() {
        String g = new InterfaceFeatures.Middle().greet();
        assertTrue(g.contains("Left"));
        assertTrue(g.contains("Right"));
    }

    // -----------------------------------------------------------------------
    // Product — Comparable + Printable
    // -----------------------------------------------------------------------
    @Test
    void product_compareTo_by_price() {
        InterfaceFeatures.Product cheap    = new InterfaceFeatures.Product("A", 100, 5);
        InterfaceFeatures.Product expensive = new InterfaceFeatures.Product("B", 500, 3);
        assertTrue(cheap.compareTo(expensive) < 0);
        assertTrue(expensive.compareTo(cheap) > 0);
        assertEquals(0, cheap.compareTo(new InterfaceFeatures.Product("C", 100, 8)));
    }

    @Test
    void product_format_contains_name_and_price() {
        String f = new InterfaceFeatures.Product("Laptop", 75_000, 10).format();
        assertTrue(f.contains("Laptop"));
        assertTrue(f.contains("75000"));
    }

    // -----------------------------------------------------------------------
    // NumberRange — Iterable
    // -----------------------------------------------------------------------
    @Test
    void number_range_generates_correct_values() {
        InterfaceFeatures.NumberRange r = new InterfaceFeatures.NumberRange(2, 10, 2);
        List<Integer> collected = new java.util.ArrayList<>();
        for (int n : r) collected.add(n);
        assertEquals(List.of(2, 4, 6, 8, 10), collected);
    }

    @Test
    void number_range_step_zero_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new InterfaceFeatures.NumberRange(1, 10, 0));
    }

    @Test
    void number_range_iterator_no_such_element_when_exhausted() {
        InterfaceFeatures.NumberRange r = new InterfaceFeatures.NumberRange(1, 1, 1);
        Iterator<Integer> it = r.iterator();
        assertTrue(it.hasNext());
        it.next();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }
}
