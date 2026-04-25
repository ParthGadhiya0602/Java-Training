package com.javatraining.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class EnumBehaviorTest {

    // -----------------------------------------------------------------------
    // Operation - abstract apply()
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "ADD,      10, 3, 13.0",
        "SUBTRACT, 10, 3,  7.0",
        "MULTIPLY, 10, 3, 30.0",
        "DIVIDE,   10, 4,  2.5",
        "MODULO,   10, 3,  1.0",
        "POWER,     2, 8, 256.0",
    })
    void operation_apply(String name, double x, double y, double expected) {
        EnumBehavior.Operation op = EnumBehavior.Operation.valueOf(name);
        assertEquals(expected, op.apply(x, y), 1e-9);
    }

    @Test
    void divide_by_zero_throws() {
        assertThrows(ArithmeticException.class,
            () -> EnumBehavior.Operation.DIVIDE.apply(5, 0));
    }

    @Test
    void modulo_by_zero_throws() {
        assertThrows(ArithmeticException.class,
            () -> EnumBehavior.Operation.MODULO.apply(5, 0));
    }

    @Test
    void operation_toString_returns_symbol() {
        assertEquals("+", EnumBehavior.Operation.ADD.toString());
        assertEquals("÷", EnumBehavior.Operation.DIVIDE.toString());
        assertEquals("^", EnumBehavior.Operation.POWER.toString());
    }

    // -----------------------------------------------------------------------
    // ProductCategory - implements Taxable
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "FOOD,        0.05, 1050.0",
        "ELECTRONICS, 0.18, 1180.0",
        "LUXURY,      0.28, 1280.0",
        "MEDICINE,    0.00, 1000.0",
        "SERVICES,    0.18, 1180.0",
    })
    void productCategory_taxRate_and_withTax(String name,
                                              double rate, double withTax) {
        EnumBehavior.ProductCategory cat = EnumBehavior.ProductCategory.valueOf(name);
        assertEquals(rate,    cat.taxRate(),        1e-9);
        assertEquals(withTax, cat.withTax(1000.0),  1e-9);
    }

    @Test
    void medicine_is_exempt_from_tax() {
        assertEquals(0.0, EnumBehavior.ProductCategory.MEDICINE.taxRate(), 1e-9);
        assertEquals(500.0, EnumBehavior.ProductCategory.MEDICINE.withTax(500.0), 1e-9);
    }

    // -----------------------------------------------------------------------
    // DiscountStrategy - abstract apply(), savings()
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "NONE,       1000.0, 1000.0,   0.0",
        "SEASONAL,   1000.0,  900.0, 100.0",
        "STUDENT,    1000.0,  800.0, 200.0",
        "SENIOR,     1000.0,  750.0, 250.0",
        "EMPLOYEE,   1000.0,  600.0, 400.0",
        "FLASH_SALE, 1000.0,  500.0, 500.0",
    })
    void discountStrategy_apply_and_savings(String name,
                                             double price,
                                             double finalPrice,
                                             double savings) {
        EnumBehavior.DiscountStrategy d = EnumBehavior.DiscountStrategy.valueOf(name);
        assertEquals(finalPrice, d.apply(price),   1e-9);
        assertEquals(savings,    d.savings(price), 1e-9);
    }

    // -----------------------------------------------------------------------
    // Season - abstract avgTempCelsius(), default avgTempFahrenheit()
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "SPRING, 22.0",
        "SUMMER, 36.0",
        "AUTUMN, 18.0",
        "WINTER, 10.0",
    })
    void season_avgTempCelsius(String name, double celsius) {
        EnumBehavior.Season s = EnumBehavior.Season.valueOf(name);
        assertEquals(celsius, s.avgTempCelsius(), 1e-9);
    }

    @Test
    void season_fahrenheit_conversion() {
        // F = C * 9/5 + 32
        for (EnumBehavior.Season s : EnumBehavior.Season.values()) {
            double expected = s.avgTempCelsius() * 9.0 / 5.0 + 32;
            assertEquals(expected, s.avgTempFahrenheit(), 1e-9);
        }
    }

    @Test
    void summer_is_hottest_season() {
        double maxTemp = Double.MIN_VALUE;
        for (EnumBehavior.Season s : EnumBehavior.Season.values()) {
            maxTemp = Math.max(maxTemp, s.avgTempCelsius());
        }
        assertEquals(maxTemp, EnumBehavior.Season.SUMMER.avgTempCelsius(), 1e-9);
    }
}
