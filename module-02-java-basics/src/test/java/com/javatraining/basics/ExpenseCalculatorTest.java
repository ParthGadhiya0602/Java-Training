package com.javatraining.basics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpenseCalculator — practical type and operator tests")
class ExpenseCalculatorTest {

    @Test
    @DisplayName("GST is exactly 18% with HALF_UP rounding")
    void gstCalculationIsExact() {
        BigDecimal[] result = ExpenseCalculator.calculateExpense(1000.00, 1);
        // 1000 * 0.18 = 180.00 exactly
        assertEquals(new BigDecimal("180.00"), result[1]);
        assertEquals(new BigDecimal("1180.00"), result[2]);
    }

    @ParameterizedTest(name = "₹{0} split {1} ways = ₹{2} per person")
    @CsvSource({
        "1180.00, 4, 295.00",   // exact split
        "1000.00, 3, 393.33",   // 1180/3 = 393.333... → rounds to 393.33
        "100.00,  7, 16.86",    // 118/7 = 16.857... → rounds to 16.86
    })
    @DisplayName("Per-person split uses HALF_UP rounding")
    void perPersonSplitRoundsCorrectly(double amount, int people, String expected) {
        BigDecimal[] result = ExpenseCalculator.calculateExpense(amount, people);
        assertEquals(new BigDecimal(expected), result[3]);
    }

    @Test
    @DisplayName("Status flags can be combined and individually tested")
    void statusFlagsCombineCorrectly() {
        int status = ExpenseCalculator.createStatus(true, true);

        assertTrue((status & ExpenseCalculator.STATUS_PENDING)    != 0);
        assertTrue((status & ExpenseCalculator.STATUS_APPROVED)   != 0);
        assertTrue((status & ExpenseCalculator.STATUS_REIMBURSED) != 0);
        assertFalse((status & ExpenseCalculator.STATUS_REJECTED)  != 0);
    }

    @Test
    @DisplayName("Status description lists all active flags as readable text")
    void statusDescriptionContainsActiveFlags() {
        int status = ExpenseCalculator.createStatus(true, false);
        String desc = ExpenseCalculator.describeStatus(status);

        assertTrue(desc.contains("PENDING"));
        assertTrue(desc.contains("APPROVED"));
        assertFalse(desc.contains("REIMBURSED"));
        assertFalse(desc.contains("REJECTED"));
    }

    @Test
    @DisplayName("Multi-category detection using Integer.bitCount")
    void categoryFlagsDetectMultipleCategories() {
        int single = ExpenseCalculator.CAT_TRAVEL;
        int multi  = ExpenseCalculator.CAT_TRAVEL | ExpenseCalculator.CAT_FOOD;
        int none   = 0;

        assertEquals("Single-category", ExpenseCalculator.expenseType(single));
        assertEquals("Multi-category",  ExpenseCalculator.expenseType(multi));
        assertEquals("Uncategorized",   ExpenseCalculator.expenseType(none));
    }
}
