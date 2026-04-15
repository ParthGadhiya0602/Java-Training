package com.javatraining.enums;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EnumCollectionsTest {

    // convenience aliases
    private static final EnumCollections.Permission READ   = EnumCollections.Permission.READ;
    private static final EnumCollections.Permission WRITE  = EnumCollections.Permission.WRITE;
    private static final EnumCollections.Permission DELETE = EnumCollections.Permission.DELETE;
    private static final EnumCollections.Permission ADMIN  = EnumCollections.Permission.ADMIN;
    private static final EnumCollections.Permission AUDIT  = EnumCollections.Permission.AUDIT;

    // -----------------------------------------------------------------------
    // EnumSet factory methods
    // -----------------------------------------------------------------------
    @Test
    void noneOf_is_empty() {
        EnumSet<EnumCollections.Permission> s =
            EnumSet.noneOf(EnumCollections.Permission.class);
        assertTrue(s.isEmpty());
    }

    @Test
    void allOf_contains_every_constant() {
        EnumSet<EnumCollections.Permission> all =
            EnumSet.allOf(EnumCollections.Permission.class);
        for (EnumCollections.Permission p : EnumCollections.Permission.values()) {
            assertTrue(all.contains(p));
        }
        assertEquals(EnumCollections.Permission.values().length, all.size());
    }

    @Test
    void range_includes_endpoints_and_intermediates() {
        EnumSet<EnumCollections.Permission> basic =
            EnumSet.range(READ, DELETE);
        assertTrue(basic.contains(READ));
        assertTrue(basic.contains(WRITE));
        assertTrue(basic.contains(DELETE));
        assertFalse(basic.contains(ADMIN));
        assertFalse(basic.contains(AUDIT));
    }

    @Test
    void complementOf_is_logical_inverse() {
        EnumSet<EnumCollections.Permission> privileged =
            EnumSet.of(DELETE, ADMIN);
        EnumSet<EnumCollections.Permission> nonPrivileged =
            EnumSet.complementOf(privileged);

        assertFalse(nonPrivileged.contains(DELETE));
        assertFalse(nonPrivileged.contains(ADMIN));
        assertTrue(nonPrivileged.contains(READ));
        assertTrue(nonPrivileged.contains(WRITE));
        assertTrue(nonPrivileged.contains(AUDIT));
    }

    @Test
    void iteration_order_matches_declaration_order() {
        EnumSet<EnumCollections.Permission> all =
            EnumSet.allOf(EnumCollections.Permission.class);
        EnumCollections.Permission[] expected = EnumCollections.Permission.values();
        int i = 0;
        for (EnumCollections.Permission p : all) {
            assertSame(expected[i++], p);
        }
    }

    // -----------------------------------------------------------------------
    // Role-based access control
    // -----------------------------------------------------------------------
    @Test
    void viewer_can_only_read() {
        EnumCollections.Role viewer = EnumCollections.Role.VIEWER;
        assertTrue(viewer.can(READ));
        assertFalse(viewer.can(WRITE));
        assertFalse(viewer.can(DELETE));
        assertFalse(viewer.can(ADMIN));
        assertFalse(viewer.can(AUDIT));
    }

    @Test
    void editor_can_read_and_write_but_not_delete() {
        EnumCollections.Role editor = EnumCollections.Role.EDITOR;
        assertTrue(editor.can(READ));
        assertTrue(editor.can(WRITE));
        assertFalse(editor.can(DELETE));
    }

    @Test
    void admin_has_all_permissions() {
        EnumCollections.Role admin = EnumCollections.Role.ADMIN;
        for (EnumCollections.Permission p : EnumCollections.Permission.values()) {
            assertTrue(admin.can(p), "ADMIN should have " + p);
        }
    }

    @Test
    void permissions_returns_defensive_copy() {
        EnumSet<EnumCollections.Permission> copy =
            EnumCollections.Role.VIEWER.permissions();
        copy.add(DELETE); // mutate the copy
        assertFalse(EnumCollections.Role.VIEWER.can(DELETE),
            "Mutating returned set must not affect the role");
    }

    // -----------------------------------------------------------------------
    // EnumMap
    // -----------------------------------------------------------------------
    @Test
    void enumMap_preserves_declaration_order() {
        EnumMap<EnumCollections.Quarter, Double> revenue =
            new EnumMap<>(EnumCollections.Quarter.class);
        revenue.put(EnumCollections.Quarter.Q3, 3.0);
        revenue.put(EnumCollections.Quarter.Q1, 1.0);
        revenue.put(EnumCollections.Quarter.Q2, 2.0);
        revenue.put(EnumCollections.Quarter.Q4, 4.0);

        List<EnumCollections.Quarter> keys = new ArrayList<>(revenue.keySet());
        assertEquals(EnumCollections.Quarter.Q1, keys.get(0));
        assertEquals(EnumCollections.Quarter.Q2, keys.get(1));
        assertEquals(EnumCollections.Quarter.Q3, keys.get(2));
        assertEquals(EnumCollections.Quarter.Q4, keys.get(3));
    }

    // -----------------------------------------------------------------------
    // frequency()
    // -----------------------------------------------------------------------
    @Test
    void frequency_counts_are_correct() {
        List<EnumCollections.Weekday> days = List.of(
            EnumCollections.Weekday.MON,
            EnumCollections.Weekday.WED,
            EnumCollections.Weekday.MON,
            EnumCollections.Weekday.FRI,
            EnumCollections.Weekday.MON
        );
        EnumMap<EnumCollections.Weekday, Long> freq =
            EnumCollections.frequency(days, EnumCollections.Weekday.class);

        assertEquals(3L, freq.get(EnumCollections.Weekday.MON));
        assertEquals(1L, freq.get(EnumCollections.Weekday.WED));
        assertEquals(1L, freq.get(EnumCollections.Weekday.FRI));
        assertNull(freq.get(EnumCollections.Weekday.SAT));
    }

    @Test
    void frequency_empty_list_returns_empty_map() {
        EnumMap<EnumCollections.Weekday, Long> freq =
            EnumCollections.frequency(List.of(), EnumCollections.Weekday.class);
        assertTrue(freq.isEmpty());
    }
}
