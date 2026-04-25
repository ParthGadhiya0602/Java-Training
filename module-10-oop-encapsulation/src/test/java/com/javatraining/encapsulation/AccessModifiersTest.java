package com.javatraining.encapsulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessModifiersTest {

    // -----------------------------------------------------------------------
    // BankAccount - invariant enforcement
    // -----------------------------------------------------------------------
    @Test
    void deposit_increases_balance() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 1_000, 500);
        acc.deposit(2_000);
        assertEquals(3_000.0, acc.balance(), 1e-9);
    }

    @Test
    void withdraw_decreases_balance_and_tracks_daily() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 5_000, 3_000);
        acc.withdraw(1_500);
        assertEquals(3_500.0, acc.balance(), 1e-9);
        assertEquals(1_500.0, acc.withdrawnToday(), 1e-9);
        assertEquals(1_500.0, acc.remainingLimit(), 1e-9);
    }

    @Test
    void withdraw_enforces_daily_limit() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 10_000, 2_000);
        acc.withdraw(2_000);
        assertThrows(IllegalStateException.class, () -> acc.withdraw(1));
    }

    @Test
    void withdraw_enforces_balance() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 500, 10_000);
        assertThrows(IllegalStateException.class, () -> acc.withdraw(600));
    }

    @Test
    void reset_daily_allows_further_withdrawal() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 10_000, 1_000);
        acc.withdraw(1_000);
        acc.resetDailyWithdrawals();  // package-private - accessible in same package
        acc.withdraw(1_000);          // should succeed after reset
        assertEquals(8_000.0, acc.balance(), 1e-9);
    }

    @Test
    void blank_account_id_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new AccessModifiers.BankAccount("", "Alice", 1_000, 500));
    }

    @Test
    void negative_initial_balance_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new AccessModifiers.BankAccount("A1", "Alice", -100, 500));
    }

    @Test
    void zero_daily_limit_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new AccessModifiers.BankAccount("A1", "Alice", 1_000, 0));
    }

    @Test
    void set_daily_limit_updates_correctly() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 5_000, 1_000);
        acc.setDailyLimit(3_000);
        assertEquals(3_000.0, acc.dailyLimit(), 1e-9);
    }

    @Test
    void negative_deposit_throws() {
        AccessModifiers.BankAccount acc =
            new AccessModifiers.BankAccount("A1", "Alice", 1_000, 500);
        assertThrows(IllegalArgumentException.class, () -> acc.deposit(-1));
    }

    // -----------------------------------------------------------------------
    // Temperature - factories and subclass access
    // -----------------------------------------------------------------------
    @Test
    void celsius_factory_converts_correctly() {
        AccessModifiers.Temperature t = AccessModifiers.Temperature.ofCelsius(100);
        assertEquals(100.0, t.toCelsius(), 1e-9);
        assertEquals(373.15, t.toKelvin(), 1e-9);
        assertEquals(212.0, t.toFahrenheit(), 1e-9);
    }

    @Test
    void kelvin_factory() {
        AccessModifiers.Temperature t = AccessModifiers.Temperature.ofKelvin(0);
        assertEquals(-273.15, t.toCelsius(), 1e-9);
    }

    @Test
    void below_absolute_zero_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> AccessModifiers.Temperature.ofCelsius(-300));
    }

    @Test
    void negative_kelvin_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> AccessModifiers.Temperature.ofKelvin(-1));
    }

    @Test
    void display_temperature_formats_in_celsius() {
        AccessModifiers.Temperature base = AccessModifiers.Temperature.ofCelsius(37);
        AccessModifiers.DisplayTemperature dt =
            new AccessModifiers.DisplayTemperature(base, "C");
        assertTrue(dt.toString().contains("37.00"));
        assertTrue(dt.toString().contains("°C"));
    }

    @Test
    void display_temperature_formats_in_fahrenheit() {
        AccessModifiers.Temperature base = AccessModifiers.Temperature.ofCelsius(0);
        AccessModifiers.DisplayTemperature dt =
            new AccessModifiers.DisplayTemperature(base, "F");
        assertTrue(dt.toString().contains("32.00"));
        assertTrue(dt.toString().contains("°F"));
    }
}
