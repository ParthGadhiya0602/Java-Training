package com.javatraining.controlflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Switch - expression, yield, pattern matching")
class SwitchTest {

    @ParameterizedTest(name = "month={0} → season={1}")
    @CsvSource({
        "1,  Winter", "2,  Winter", "12, Winter",
        "3,  Spring", "4,  Spring", "5,  Spring",
        "6,  Summer", "7,  Summer", "8,  Summer",
        "9,  Autumn", "10, Autumn", "11, Autumn",
    })
    @DisplayName("seasonOfModern covers all 12 months correctly")
    void seasonAllMonths(int month, String expected) {
        assertEquals(expected, SwitchDemo.seasonOfModern(month));
    }

    @Test
    @DisplayName("Invalid month throws IllegalArgumentException")
    void invalidMonth() {
        assertThrows(IllegalArgumentException.class,
            () -> SwitchDemo.seasonOfModern(13));
        assertThrows(IllegalArgumentException.class,
            () -> SwitchDemo.seasonOfModern(0));
    }

    @ParameterizedTest(name = "month={0} year={1} → days={2}")
    @CsvSource({
        "1,  2024, 31",   // January
        "4,  2024, 30",   // April
        "2,  2024, 29",   // Feb 2024 - leap year
        "2,  2023, 28",   // Feb 2023 - not leap
        "2,  2100, 28",   // Feb 2100 - divisible by 100 but not 400
        "2,  2000, 29",   // Feb 2000 - divisible by 400
    })
    @DisplayName("daysInMonth handles leap year rules correctly")
    void daysInMonth(int month, int year, int expected) {
        assertEquals(expected, SwitchDemo.daysInMonth(month, year));
    }

    @Test
    @DisplayName("formatNotification dispatches on sealed subtypes exhaustively")
    void notificationFormatting() {
        var email = new SwitchDemo.EmailNotification("a@b.com", "Hello");
        var sms   = new SwitchDemo.SmsNotification("+1-555-0100", "Hi");
        var push  = new SwitchDemo.PushNotification("dev-xyz", "Alert");

        assertTrue(SwitchDemo.formatNotification(email).contains("a@b.com"));
        assertTrue(SwitchDemo.formatNotification(sms).contains("+1-555-0100"));
        assertTrue(SwitchDemo.formatNotification(push).contains("dev-xyz"));
    }
}
