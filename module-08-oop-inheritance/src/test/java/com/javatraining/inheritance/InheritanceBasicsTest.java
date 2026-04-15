package com.javatraining.inheritance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class InheritanceBasicsTest {

    // -----------------------------------------------------------------------
    // Vehicle / Car / ElectricCar
    // -----------------------------------------------------------------------
    @Test
    void vehicle_initial_speed_is_zero() {
        InheritanceBasics.Vehicle v = new InheritanceBasics.Vehicle("X", "Y", 2020);
        assertEquals(0, v.speed());
    }

    @Test
    void car_default_doors_is_four() {
        InheritanceBasics.Car c = new InheritanceBasics.Car("Toyota", "Camry", 2022);
        assertEquals(4, c.doors());
    }

    @Test
    void car_accelerate_updates_speed() {
        InheritanceBasics.Car c = new InheritanceBasics.Car("Toyota", "Camry", 2022);
        c.accelerate(60);
        assertEquals(60, c.speed());
    }

    @Test
    void car_brake_clamps_at_zero() {
        InheritanceBasics.Car c = new InheritanceBasics.Car("Toyota", "Camry", 2022);
        c.accelerate(30);
        c.brake(50);  // more than current speed
        assertEquals(0, c.speed());
    }

    @Test
    void electric_car_fuel_type_is_electric() {
        InheritanceBasics.ElectricCar ev =
            new InheritanceBasics.ElectricCar("Tesla", "M3", 2023, 75);
        assertEquals("Electric", ev.fuelType());
    }

    @Test
    void electric_car_accelerate_drains_battery() {
        InheritanceBasics.ElectricCar ev =
            new InheritanceBasics.ElectricCar("Tesla", "M3", 2023, 75);
        ev.accelerate(60);  // drains 60/10 = 6%
        assertEquals(94, ev.chargePercent());
    }

    @Test
    void electric_car_charge_clamps_at_100() {
        InheritanceBasics.ElectricCar ev =
            new InheritanceBasics.ElectricCar("Tesla", "M3", 2023, 75);
        ev.charge(200);  // should not exceed 100%
        assertEquals(100, ev.chargePercent());
    }

    @Test
    void label_includes_year_make_model() {
        InheritanceBasics.Vehicle v = new InheritanceBasics.Vehicle("Honda", "City", 2021);
        assertTrue(v.label().contains("Honda"));
        assertTrue(v.label().contains("City"));
        assertTrue(v.label().contains("2021"));
    }

    // -----------------------------------------------------------------------
    // ImmutableMoney
    // -----------------------------------------------------------------------
    @ParameterizedTest
    @CsvSource({
        "10000, 5000, 15000",
        "500,   500,  1000",
    })
    void money_add(double a, double b, double expected) {
        InheritanceBasics.ImmutableMoney ma = InheritanceBasics.ImmutableMoney.ofRupees(a, "INR");
        InheritanceBasics.ImmutableMoney mb = InheritanceBasics.ImmutableMoney.ofRupees(b, "INR");
        assertEquals(expected, ma.add(mb).toRupees(), 1e-9);
    }

    @Test
    void money_subtract() {
        InheritanceBasics.ImmutableMoney a = InheritanceBasics.ImmutableMoney.ofRupees(5000, "INR");
        InheritanceBasics.ImmutableMoney b = InheritanceBasics.ImmutableMoney.ofRupees(1500, "INR");
        assertEquals(3500.0, a.subtract(b).toRupees(), 1e-9);
    }

    @Test
    void money_subtract_below_zero_throws() {
        InheritanceBasics.ImmutableMoney a = InheritanceBasics.ImmutableMoney.ofRupees(100, "INR");
        InheritanceBasics.ImmutableMoney b = InheritanceBasics.ImmutableMoney.ofRupees(200, "INR");
        assertThrows(ArithmeticException.class, () -> a.subtract(b));
    }

    @Test
    void money_currency_mismatch_throws() {
        InheritanceBasics.ImmutableMoney inr = InheritanceBasics.ImmutableMoney.ofRupees(100, "INR");
        InheritanceBasics.ImmutableMoney usd = InheritanceBasics.ImmutableMoney.ofRupees(100, "USD");
        assertThrows(IllegalArgumentException.class, () -> inr.add(usd));
    }

    @Test
    void money_multiply() {
        InheritanceBasics.ImmutableMoney m = InheritanceBasics.ImmutableMoney.ofRupees(1000, "INR");
        assertEquals(1500.0, m.multiply(1.5).toRupees(), 0.01);
    }

    @Test
    void money_is_immutable_add_returns_new_object() {
        InheritanceBasics.ImmutableMoney original = InheritanceBasics.ImmutableMoney.ofRupees(1000, "INR");
        InheritanceBasics.ImmutableMoney added    = original.add(
            InheritanceBasics.ImmutableMoney.ofRupees(500, "INR"));
        assertEquals(1000.0, original.toRupees(), 1e-9); // unchanged
        assertEquals(1500.0, added.toRupees(),    1e-9);
    }

    @Test
    void money_equals_and_hashcode() {
        InheritanceBasics.ImmutableMoney a = InheritanceBasics.ImmutableMoney.ofRupees(500, "INR");
        InheritanceBasics.ImmutableMoney b = InheritanceBasics.ImmutableMoney.ofRupees(500, "INR");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
