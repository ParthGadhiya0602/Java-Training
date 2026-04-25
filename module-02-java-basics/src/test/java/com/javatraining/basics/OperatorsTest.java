package com.javatraining.basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that verify the non-obvious behaviours discussed in Operators.java.
 * Run with: mvn test
 */
@DisplayName("Operators - non-obvious behaviour verification")
class OperatorsTest {

    @Test
    @DisplayName("Integer division truncates toward zero, not toward negative infinity")
    void integerDivisionTruncatesTowardZero() {
        assertEquals(3,  7 / 2);    // not 3.5
        assertEquals(-3, -7 / 2);   // truncate toward zero: -3, not -4
        assertEquals(-3, 7 / -2);   // truncate toward zero: -3, not -4
    }

    @Test
    @DisplayName("Modulo result has same sign as dividend in Java")
    void moduloSignFollowsDividend() {
        assertEquals(1,  7 % 3);    // positive dividend → positive remainder
        assertEquals(-1, -7 % 3);   // negative dividend → negative remainder
        assertEquals(1,  7 % -3);   // sign of divisor is irrelevant
    }

    @Test
    @DisplayName("Floating-point addition is not exact - 0.1 + 0.2 != 0.3")
    void floatingPointImprecision() {
        double result = 0.1 + 0.2;
        assertNotEquals(0.3, result); // This PASSES - they are NOT equal
        // Correct way: compare with a tolerance
        assertEquals(0.3, result, 1e-9);
    }

    @Test
    @DisplayName("Short-circuit AND skips right side when left is false")
    void shortCircuitAndSkipsRightSide() {
        int[] counter = {0}; // array trick to mutate inside lambda

        // This should NOT throw even though counter[0] starts at 0
        boolean result = false && (++counter[0] > 0);

        assertFalse(result);
        assertEquals(0, counter[0], "Right side should not have been evaluated");
    }

    @Test
    @DisplayName("Short-circuit OR skips right side when left is true")
    void shortCircuitOrSkipsRightSide() {
        int[] counter = {0};
        boolean result = true || (++counter[0] > 0);

        assertTrue(result);
        assertEquals(0, counter[0], "Right side should not have been evaluated");
    }

    @Test
    @DisplayName("Integer.MAX_VALUE + 1 silently overflows to MIN_VALUE")
    void integerOverflowIssilent() {
        int overflowed = Integer.MAX_VALUE + 1;
        assertEquals(Integer.MIN_VALUE, overflowed);
    }

    @Test
    @DisplayName("Math.addExact throws on overflow instead of silently wrapping")
    void mathAddExactThrowsOnOverflow() {
        assertThrows(ArithmeticException.class,
            () -> Math.addExact(Integer.MAX_VALUE, 1));
    }

    @Test
    @DisplayName("Left shift is equivalent to multiplication by power of 2")
    void leftShiftIsMultiplication() {
        assertEquals(1 * 8,  1 << 3);   // 2^3 = 8
        assertEquals(5 * 16, 5 << 4);   // 2^4 = 16
    }

    @Test
    @DisplayName("Integer cache: == works for -128..127 but not beyond")
    void integerCacheRange() {
        Integer a = 127;
        Integer b = 127;
        assertTrue(a == b, "Cached range: same object reference");

        Integer c = 128;
        Integer d = 128;
        assertFalse(c == d, "Outside cache: different objects");
        assertTrue(c.equals(d), "equals() compares value, not reference");
    }

    @Test
    @DisplayName("instanceof pattern matching binds variable in one step")
    void instanceofPatternMatching() {
        Object obj = "hello";

        if (obj instanceof String s) {
            assertEquals(5, s.length()); // s is String, no cast needed
        } else {
            fail("Should have matched String");
        }
    }

    @Test
    @DisplayName("Bitwise flags: set, check, and clear individual bits")
    void bitwiseFlagOperations() {
        final int READ    = 0b001;
        final int WRITE   = 0b010;
        final int EXECUTE = 0b100;

        int perms = 0;
        perms |= READ | WRITE;  // grant read + write

        assertTrue((perms & READ)    != 0, "should have READ");
        assertTrue((perms & WRITE)   != 0, "should have WRITE");
        assertFalse((perms & EXECUTE) != 0, "should NOT have EXECUTE");

        perms &= ~WRITE;  // revoke write

        assertTrue((perms & READ)    != 0, "should still have READ");
        assertFalse((perms & WRITE)  != 0, "should no longer have WRITE");
    }
}
