package com.javatraining.methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VarargsDemo - variable-length argument lists")
class VarargsTest {

    @Test
    @DisplayName("sum() with zero args returns 0")
    void sumEmpty() {
        assertEquals(0, VarargsDemo.sum());
    }

    @Test
    @DisplayName("sum() with multiple args returns correct total")
    void sumMultiple() {
        assertEquals(150, VarargsDemo.sum(10, 20, 30, 40, 50));
    }

    @Test
    @DisplayName("sum() accepts an explicit array")
    void sumArray() {
        assertEquals(18, VarargsDemo.sum(new int[]{3, 6, 9}));
    }

    @Test
    @DisplayName("average() computes correctly")
    void average() {
        assertEquals(3.0, VarargsDemo.average(1.0, 2.0, 3.0, 4.0, 5.0), 0.001);
    }

    @Test
    @DisplayName("average() throws for empty input")
    void averageEmpty() {
        assertThrows(IllegalArgumentException.class, () -> VarargsDemo.average());
    }

    @Test
    @DisplayName("join() concatenates with separator")
    void join() {
        assertEquals("Alice, Bob, Charlie",
            VarargsDemo.join(", ", "Alice", "Bob", "Charlie"));
    }

    @Test
    @DisplayName("join() with single part has no separator")
    void joinSinglePart() {
        assertEquals("Only", VarargsDemo.join("-", "Only"));
    }

    @Test
    @DisplayName("join() with no parts returns empty string")
    void joinNoParts() {
        assertEquals("", VarargsDemo.join("-"));
    }

    @Test
    @DisplayName("build() replaces all placeholders correctly")
    void buildTemplate() {
        assertEquals("Hello, Alice! You have 5 messages.",
            VarargsDemo.build("Hello, {}! You have {} messages.", "Alice", 5));
    }

    @Test
    @DisplayName("build() throws when argument count mismatches placeholder count")
    void buildMismatch() {
        assertThrows(IllegalArgumentException.class,
            () -> VarargsDemo.build("Hello, {}!", "a", "extra"));
    }

    @Test
    @DisplayName("stats() returns correct min, max, sum, average")
    void stats() {
        VarargsDemo.Stats s = VarargsDemo.stats(3.0, 1.0, 7.0, 5.0, 9.0);
        assertEquals(5,   s.count());
        assertEquals(1.0, s.min(),     0.001);
        assertEquals(9.0, s.max(),     0.001);
        assertEquals(25.0, s.sum(),    0.001);
        assertEquals(5.0, s.average(), 0.001);
    }

    @Test
    @DisplayName("stats() with single value has min==max==average")
    void statsSingle() {
        VarargsDemo.Stats s = VarargsDemo.stats(42.0);
        assertEquals(1,    s.count());
        assertEquals(42.0, s.min(),     0.001);
        assertEquals(42.0, s.max(),     0.001);
        assertEquals(42.0, s.average(), 0.001);
    }
}
