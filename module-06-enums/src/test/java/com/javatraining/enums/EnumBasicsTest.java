package com.javatraining.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class EnumBasicsTest {

    // -----------------------------------------------------------------------
    // Direction — built-in enum methods
    // -----------------------------------------------------------------------
    @Test
    void direction_ordinal_follows_declaration_order() {
        assertEquals(0, EnumBasics.Direction.NORTH.ordinal());
        assertEquals(1, EnumBasics.Direction.SOUTH.ordinal());
        assertEquals(2, EnumBasics.Direction.EAST.ordinal());
        assertEquals(3, EnumBasics.Direction.WEST.ordinal());
    }

    @Test
    void direction_name_and_valueOf_roundtrip() {
        for (EnumBasics.Direction d : EnumBasics.Direction.values()) {
            assertEquals(d, EnumBasics.Direction.valueOf(d.name()));
        }
    }

    @Test
    void direction_singleton_equality() {
        EnumBasics.Direction a = EnumBasics.Direction.valueOf("EAST");
        assertSame(EnumBasics.Direction.EAST, a);   // == is safe for enums
    }

    // -----------------------------------------------------------------------
    // HttpStatus — fields, predicates, reverse lookup
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "200, OK,                    true,  false, false",
        "201, Created,               true,  false, false",
        "404, Not Found,             false, true,  false",
        "500, Internal Server Error, false, false, true",
        "503, Service Unavailable,   false, false, true",
    })
    void httpStatus_predicates(int code, String reason,
                               boolean success, boolean client, boolean server) {
        EnumBasics.HttpStatus s = EnumBasics.HttpStatus.fromCode(code);
        assertEquals(code,   s.code());
        assertEquals(reason, s.reason());
        assertEquals(success, s.isSuccess());
        assertEquals(client,  s.isClientError());
        assertEquals(server,  s.isServerError());
    }

    @Test
    void httpStatus_fromCode_returns_correct_constant() {
        assertSame(EnumBasics.HttpStatus.NOT_FOUND,      EnumBasics.HttpStatus.fromCode(404));
        assertSame(EnumBasics.HttpStatus.INTERNAL_ERROR, EnumBasics.HttpStatus.fromCode(500));
    }

    @Test
    void httpStatus_fromCode_throws_for_unknown() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> EnumBasics.HttpStatus.fromCode(999)
        );
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void httpStatus_toString_includes_code_and_reason() {
        assertEquals("404 Not Found", EnumBasics.HttpStatus.NOT_FOUND.toString());
        assertEquals("200 OK",        EnumBasics.HttpStatus.OK.toString());
    }

    // -----------------------------------------------------------------------
    // Planet — computed properties
    // -----------------------------------------------------------------------
    @Test
    void earth_surface_weight_matches_input_mass() {
        // surfaceWeight(mass/g) = mass/g * g = mass (in Newtons at 1 g)
        double bodyMassKg = 75.0;
        double earthG = EnumBasics.Planet.EARTH.surfaceGravity();
        double weightOnEarth = EnumBasics.Planet.EARTH.surfaceWeight(bodyMassKg / earthG);
        assertEquals(bodyMassKg, weightOnEarth, 1e-6);
    }

    @Test
    void jupiter_gravity_is_highest() {
        double maxG = 0;
        for (EnumBasics.Planet p : EnumBasics.Planet.values()) {
            maxG = Math.max(maxG, p.surfaceGravity());
        }
        assertEquals(maxG, EnumBasics.Planet.JUPITER.surfaceGravity(), 1e-6);
    }

    @Test
    void all_planets_have_positive_surface_gravity() {
        for (EnumBasics.Planet p : EnumBasics.Planet.values()) {
            assertTrue(p.surfaceGravity() > 0,
                p + " should have positive surface gravity");
        }
    }
}
