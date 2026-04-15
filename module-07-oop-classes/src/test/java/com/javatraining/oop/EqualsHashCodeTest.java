package com.javatraining.oop;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EqualsHashCodeTest {

    // -----------------------------------------------------------------------
    // Point — five contract rules
    // -----------------------------------------------------------------------
    @Test
    void reflexive() {
        EqualsHashCode.Point p = new EqualsHashCode.Point(1, 2);
        assertEquals(p, p);
    }

    @Test
    void symmetric() {
        EqualsHashCode.Point a = new EqualsHashCode.Point(3, 4);
        EqualsHashCode.Point b = new EqualsHashCode.Point(3, 4);
        assertEquals(a, b);
        assertEquals(b, a);
    }

    @Test
    void transitive() {
        EqualsHashCode.Point a = new EqualsHashCode.Point(1, 1);
        EqualsHashCode.Point b = new EqualsHashCode.Point(1, 1);
        EqualsHashCode.Point c = new EqualsHashCode.Point(1, 1);
        assertEquals(a, b);
        assertEquals(b, c);
        assertEquals(a, c);
    }

    @Test
    void null_safe() {
        EqualsHashCode.Point p = new EqualsHashCode.Point(5, 6);
        assertNotEquals(null, p);
    }

    @Test
    void unequal_points() {
        assertNotEquals(new EqualsHashCode.Point(1, 2), new EqualsHashCode.Point(1, 3));
        assertNotEquals(new EqualsHashCode.Point(1, 2), new EqualsHashCode.Point(2, 2));
    }

    @Test
    void equal_points_have_equal_hashcodes() {
        EqualsHashCode.Point a = new EqualsHashCode.Point(7, 8);
        EqualsHashCode.Point b = new EqualsHashCode.Point(7, 8);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // -----------------------------------------------------------------------
    // HashSet / HashMap behaviour with correct implementation
    // -----------------------------------------------------------------------
    @Test
    void hashset_deduplicates_equal_points() {
        Set<EqualsHashCode.Point> set = new HashSet<>();
        set.add(new EqualsHashCode.Point(1, 2));
        set.add(new EqualsHashCode.Point(1, 2)); // duplicate
        assertEquals(1, set.size());
    }

    @Test
    void hashset_contains_semantically_equal_point() {
        Set<EqualsHashCode.Point> set = new HashSet<>();
        set.add(new EqualsHashCode.Point(10, 20));
        assertTrue(set.contains(new EqualsHashCode.Point(10, 20)));
    }

    @Test
    void hashmap_lookup_works_with_new_key_object() {
        Map<EqualsHashCode.Point, String> map = new HashMap<>();
        map.put(new EqualsHashCode.Point(0, 0), "origin");
        assertEquals("origin", map.get(new EqualsHashCode.Point(0, 0)));
        assertNull(map.get(new EqualsHashCode.Point(9, 9)));
    }

    // -----------------------------------------------------------------------
    // BrokenPoint — broken hashCode makes contains unreliable
    // -----------------------------------------------------------------------
    @Test
    void broken_hashcode_returns_zero_always() {
        assertEquals(0, new EqualsHashCode.BrokenPoint(1, 2).hashCode());
        assertEquals(0, new EqualsHashCode.BrokenPoint(99, 99).hashCode());
    }

    @Test
    void broken_point_equals_is_still_correct() {
        EqualsHashCode.BrokenPoint a = new EqualsHashCode.BrokenPoint(1, 2);
        EqualsHashCode.BrokenPoint b = new EqualsHashCode.BrokenPoint(1, 2);
        assertEquals(a, b);
    }

    // -----------------------------------------------------------------------
    // toString
    // -----------------------------------------------------------------------
    @Test
    void point_toString_format() {
        assertEquals("Point(3, 4)", new EqualsHashCode.Point(3, 4).toString());
    }
}
