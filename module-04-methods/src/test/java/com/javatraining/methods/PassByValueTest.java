package com.javatraining.methods;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PassByValue - Java always passes by value")
class PassByValueTest {

    @Test
    @DisplayName("Primitive: changes inside method do NOT affect caller")
    void primitiveIsNotAffected() {
        int n = 10;
        PassByValueDemo.doubleIt(n);
        assertEquals(10, n, "Primitive must be unchanged after method call");
    }

    @Test
    @DisplayName("Object mutation: changes to fields ARE visible to caller")
    void objectMutationIsVisible() {
        int[] arr = {1, 2, 3, 4, 5};
        PassByValueDemo.appendItems(arr);
        assertArrayEquals(new int[]{10, 20, 30, 40, 50}, arr);
    }

    @Test
    @DisplayName("Reference reassignment inside method is NOT visible to caller")
    void referenceReassignmentIsInvisible() {
        int[] arr = {1, 2, 3};
        PassByValueDemo.tryToReplace(arr);
        // Original array untouched - the method only changed its local reference copy
        assertArrayEquals(new int[]{1, 2, 3}, arr);
    }

    @Test
    @DisplayName("minMaxClean returns correct min and max via record")
    void minMaxViaRecord() {
        int[] data = {5, 2, 8, 1, 9, 3};
        PassByValueDemo.MinMax mm = PassByValueDemo.minMaxClean(data);
        assertEquals(1, mm.min());
        assertEquals(9, mm.max());
    }

    @Test
    @DisplayName("Defensive copy: external array mutation does not affect internal state")
    void defensiveCopyProtectsInternalState() {
        int[] holidays = {1, 15, 26};
        PassByValueDemo.ImmutableCalendar cal = new PassByValueDemo.ImmutableCalendar(holidays);

        holidays[0] = 999;  // tamper with original
        assertEquals(1, cal.getHolidays()[0], "Internal array must be unaffected");

        int[] retrieved = cal.getHolidays();
        retrieved[0] = 888;  // tamper with returned copy
        assertEquals(1, cal.getHolidays()[0], "Getter must return a new copy each time");
    }
}
