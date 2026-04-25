package com.javatraining.inheritance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolymorphismDemoTest {

    // -----------------------------------------------------------------------
    // Dynamic dispatch - right sound() for each concrete type
    // -----------------------------------------------------------------------
    @Test
    void dog_sound_is_woof() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Dog("Rex", 3, "Lab");
        assertEquals("Woof!", a.sound());
    }

    @Test
    void cat_sound_is_meow() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Cat("Nala", 2, true);
        assertEquals("Meow~", a.sound());
    }

    @Test
    void parrot_sound_contains_phrase() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Parrot("Polly", 1, "Hello");
        assertTrue(a.sound().contains("Hello"));
    }

    // -----------------------------------------------------------------------
    // Widening / narrowing / instanceof pattern
    // -----------------------------------------------------------------------
    @Test
    void widening_reference_holds_subtype_object() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Dog("Rex", 3, "Lab");
        assertInstanceOf(PolymorphismDemo.Dog.class, a);
    }

    @Test
    void instanceof_pattern_match_binds_variable() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Dog("Rex", 3, "Lab");
        if (a instanceof PolymorphismDemo.Dog dog) {
            assertEquals("Lab", dog.breed());
        } else {
            fail("Expected Dog pattern to match");
        }
    }

    @Test
    void narrowing_cast_fails_with_class_cast_exception() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Cat("Nala", 2, true);
        assertThrows(ClassCastException.class, () -> {
            PolymorphismDemo.Dog d = (PolymorphismDemo.Dog) a;
        });
    }

    @Test
    void instanceof_returns_false_for_wrong_type() {
        PolymorphismDemo.Animal a = new PolymorphismDemo.Cat("Nala", 2, true);
        assertFalse(a instanceof PolymorphismDemo.Dog);
    }

    // -----------------------------------------------------------------------
    // LSP violation - Square breaks Rectangle contract
    // -----------------------------------------------------------------------
    @Test
    void rectangle_area_is_width_times_height() {
        PolymorphismDemo.Rectangle r = new PolymorphismDemo.Rectangle(5, 3);
        r.setWidth(5);
        r.setHeight(3);
        assertEquals(15, r.area());
    }

    @Test
    void square_lsp_violation_breaks_area_contract() {
        PolymorphismDemo.Rectangle r = new PolymorphismDemo.SquareLSPViolation(10);
        r.setWidth(5);
        r.setHeight(3);
        // setHeight forces width=3 too → area=9, not 15
        assertNotEquals(15, r.area(), "Square violates LSP - area should be 15 but isn't");
    }

    // -----------------------------------------------------------------------
    // Animal properties
    // -----------------------------------------------------------------------
    @Test
    void animal_name_and_age() {
        PolymorphismDemo.Dog d = new PolymorphismDemo.Dog("Buddy", 5, "Beagle");
        assertEquals("Buddy", d.name());
        assertEquals(5, d.age());
    }

    @Test
    void cat_indoor_flag() {
        PolymorphismDemo.Cat indoor  = new PolymorphismDemo.Cat("A", 1, true);
        PolymorphismDemo.Cat outdoor = new PolymorphismDemo.Cat("B", 2, false);
        assertTrue(indoor.isIndoor());
        assertFalse(outdoor.isIndoor());
    }
}
