package com.javatraining.oop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassAnatomyTest {

    // -----------------------------------------------------------------------
    // Counter - constructor chaining + static/instance state
    // -----------------------------------------------------------------------
    @Test
    void canonical_constructor_sets_label_and_value() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter("test", 42);
        assertEquals(42, c.value());
    }

    @Test
    void single_arg_constructor_chains_to_zero() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter("x");
        assertEquals(0, c.value());
    }

    @Test
    void no_arg_constructor_chains_to_counter_zero() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter();
        assertEquals(0, c.value());
    }

    @Test
    void increment_increases_value_by_one() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter("t", 5);
        c.increment();
        assertEquals(6, c.value());
    }

    @Test
    void increment_by_amount() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter("t", 0);
        c.increment(7);
        assertEquals(7, c.value());
    }

    @Test
    void increment_clamps_at_max() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter("t", 995);
        c.increment(100); // would exceed 1000
        assertEquals(1000, c.value());
    }

    @Test
    void reset_sets_value_to_zero() {
        ClassAnatomy.Counter c = new ClassAnatomy.Counter("t", 50);
        c.reset();
        assertEquals(0, c.value());
    }

    @Test
    void invalid_initial_value_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new ClassAnatomy.Counter("t", -1));
        assertThrows(IllegalArgumentException.class,
            () -> new ClassAnatomy.Counter("t", 1001));
    }

    @Test
    void each_counter_gets_unique_id() {
        ClassAnatomy.Counter a = new ClassAnatomy.Counter("a");
        ClassAnatomy.Counter b = new ClassAnatomy.Counter("b");
        assertNotEquals(a.id(), b.id());
    }

    // -----------------------------------------------------------------------
    // Temperature - static factories
    // -----------------------------------------------------------------------
    @Test
    void celsius_roundtrip() {
        ClassAnatomy.Temperature t = ClassAnatomy.Temperature.ofCelsius(100);
        assertEquals(100.0, t.toCelsius(), 1e-9);
    }

    @Test
    void fahrenheit_to_celsius_conversion() {
        // 32°F = 0°C
        ClassAnatomy.Temperature t = ClassAnatomy.Temperature.ofFahrenheit(32);
        assertEquals(0.0, t.toCelsius(), 1e-9);
        // 212°F = 100°C
        ClassAnatomy.Temperature boil = ClassAnatomy.Temperature.ofFahrenheit(212);
        assertEquals(100.0, boil.toCelsius(), 1e-9);
    }

    @Test
    void kelvin_conversion() {
        ClassAnatomy.Temperature t = ClassAnatomy.Temperature.ofKelvin(373.15);
        assertEquals(100.0, t.toCelsius(), 1e-6);
    }

    @Test
    void boiling_and_freezing_predicates() {
        assertTrue(ClassAnatomy.Temperature.ofCelsius(100).isBoiling());
        assertFalse(ClassAnatomy.Temperature.ofCelsius(99).isBoiling());
        assertTrue(ClassAnatomy.Temperature.ofCelsius(0).isFreezing());
        assertFalse(ClassAnatomy.Temperature.ofCelsius(1).isFreezing());
    }

    @Test
    void below_absolute_zero_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> ClassAnatomy.Temperature.ofCelsius(-300));
    }

    @Test
    void absolute_zero_is_valid() {
        ClassAnatomy.Temperature t = ClassAnatomy.Temperature.absoluteZero();
        assertEquals(-273.15, t.toCelsius(), 1e-9);
    }
}
