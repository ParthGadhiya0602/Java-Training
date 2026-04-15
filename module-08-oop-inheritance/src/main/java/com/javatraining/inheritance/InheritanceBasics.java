package com.javatraining.inheritance;

/**
 * TOPIC: Inheritance basics — extends, super, constructor chaining,
 *        method overriding, covariant return types, and final.
 *
 * Core rules:
 *   • super(...) must be the FIRST statement in a child constructor.
 *     If omitted, the compiler inserts super() — the no-arg parent constructor.
 *   • @Override forces the compiler to verify the method actually overrides something.
 *     Omitting it is legal but allows silent bugs (typo creates an overload, not override).
 *   • Covariant return: an overriding method may narrow the return type to a subtype.
 *   • final class    → cannot be extended   (String, Integer are final)
 *   • final method   → cannot be overridden
 *   • final field    → assigned once (in constructor or at declaration), never changed
 */
public class InheritanceBasics {

    // -------------------------------------------------------------------------
    // 1. Simple hierarchy: Vehicle → Car → ElectricCar
    // -------------------------------------------------------------------------
    static class Vehicle {
        private final String make;
        private final String model;
        private final int    year;
        protected int        speedKmh;  // protected: visible to subclasses

        Vehicle(String make, String model, int year) {
            this.make  = make;
            this.model = model;
            this.year  = year;
            this.speedKmh = 0;
        }

        // Can be overridden — virtual by default in Java
        void accelerate(int by) {
            speedKmh += by;
            System.out.printf("  %s accelerates to %d km/h%n", label(), speedKmh);
        }

        void brake(int by) {
            speedKmh = Math.max(0, speedKmh - by);
            System.out.printf("  %s brakes to %d km/h%n", label(), speedKmh);
        }

        // final method — no subclass may override this
        final String label() { return year + " " + make + " " + model; }

        String fuelType() { return "Petrol"; }

        int speed() { return speedKmh; }

        @Override
        public String toString() {
            return label() + " [" + fuelType() + "] speed=" + speedKmh + " km/h";
        }
    }

    static class Car extends Vehicle {
        private final int doors;

        Car(String make, String model, int year, int doors) {
            super(make, model, year);   // chain upward — must be first
            this.doors = doors;
        }

        // Convenience constructor — chains to sibling
        Car(String make, String model, int year) {
            this(make, model, year, 4);
        }

        int doors() { return doors; }

        @Override
        void accelerate(int by) {
            // Augment — call parent, then add extra behaviour
            super.accelerate(by);
            if (speedKmh > 120) {
                System.out.println("  [Car] Warning: high speed!");
            }
        }

        @Override
        public String toString() {
            return super.toString() + " doors=" + doors;
        }
    }

    static class ElectricCar extends Car {
        private final int batteryKwh;
        private       int chargePercent;

        ElectricCar(String make, String model, int year, int batteryKwh) {
            super(make, model, year, 4);
            this.batteryKwh    = batteryKwh;
            this.chargePercent = 100;
        }

        @Override
        String fuelType() { return "Electric"; }   // covariant return not shown here, just override

        @Override
        void accelerate(int by) {
            int drainPercent = by / 10;
            chargePercent = Math.max(0, chargePercent - drainPercent);
            super.accelerate(by);                  // calls Car.accelerate → Vehicle.accelerate
            System.out.printf("  [EV] Battery: %d%%%n", chargePercent);
        }

        void charge(int percent) {
            chargePercent = Math.min(100, chargePercent + percent);
            System.out.printf("  [EV] Charged to %d%%%n", chargePercent);
        }

        int chargePercent() { return chargePercent; }

        @Override
        public String toString() {
            return super.toString() + " battery=" + batteryKwh
                   + "kWh charge=" + chargePercent + "%";
        }
    }

    // -------------------------------------------------------------------------
    // 2. Covariant return type
    //    Builder returns "this" type — so Builder.withX() returns Builder,
    //    but ExtendedBuilder.withX() can return ExtendedBuilder.
    // -------------------------------------------------------------------------
    static class Builder {
        String name = "";
        int    age  = 0;

        Builder withName(String n) { this.name = n; return this; }
        Builder withAge(int a)     { this.age  = a; return this; }

        // Covariant: overriding method may return a subtype of the declared return type
        Builder build() { return this; }

        @Override public String toString() { return "Builder{name=" + name + ", age=" + age + "}"; }
    }

    static class ExtendedBuilder extends Builder {
        String email = "";

        // Covariant return — return type is ExtendedBuilder (subtype of Builder)
        @Override
        ExtendedBuilder withName(String n) { super.withName(n); return this; }

        @Override
        ExtendedBuilder withAge(int a) { super.withAge(a); return this; }

        ExtendedBuilder withEmail(String e) { this.email = e; return this; }

        @Override
        ExtendedBuilder build() { return this; }

        @Override public String toString() {
            return "ExtendedBuilder{name=" + name + ", age=" + age + ", email=" + email + "}";
        }
    }

    // -------------------------------------------------------------------------
    // 3. final class — cannot be extended
    //    ImmutableMoney is final to prevent subclasses from breaking immutability.
    // -------------------------------------------------------------------------
    static final class ImmutableMoney {
        private final long   paise;   // stores as smallest unit to avoid float imprecision
        private final String currency;

        ImmutableMoney(long paise, String currency) {
            this.paise    = paise;
            this.currency = currency;
        }

        static ImmutableMoney ofRupees(double rupees, String currency) {
            return new ImmutableMoney(Math.round(rupees * 100), currency);
        }

        ImmutableMoney add(ImmutableMoney other) {
            assertSameCurrency(other);
            return new ImmutableMoney(paise + other.paise, currency);
        }

        ImmutableMoney subtract(ImmutableMoney other) {
            assertSameCurrency(other);
            if (paise < other.paise) throw new ArithmeticException("Negative money");
            return new ImmutableMoney(paise - other.paise, currency);
        }

        ImmutableMoney multiply(double factor) {
            return new ImmutableMoney(Math.round(paise * factor), currency);
        }

        boolean isGreaterThan(ImmutableMoney other) {
            assertSameCurrency(other);
            return paise > other.paise;
        }

        double toRupees()   { return paise / 100.0; }
        String currency()   { return currency; }

        private void assertSameCurrency(ImmutableMoney other) {
            if (!currency.equals(other.currency))
                throw new IllegalArgumentException(
                    "Currency mismatch: " + currency + " vs " + other.currency);
        }

        @Override public String toString() {
            return String.format("%s %.2f", currency, toRupees());
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof ImmutableMoney m)) return false;
            return paise == m.paise && currency.equals(m.currency);
        }

        @Override public int hashCode() {
            return java.util.Objects.hash(paise, currency);
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void vehicleDemo() {
        System.out.println("=== Vehicle Hierarchy ===");

        Vehicle v    = new Vehicle("Bajaj", "Pulsar", 2020);
        Car     car  = new Car("Toyota", "Camry", 2022);
        ElectricCar ev = new ElectricCar("Tesla", "Model 3", 2023, 75);

        System.out.println(v);
        System.out.println(car);
        System.out.println(ev);

        System.out.println("\nAccelerating:");
        car.accelerate(80);
        car.accelerate(50);  // triggers warning
        ev.accelerate(60);
        ev.brake(20);
        ev.charge(10);

        System.out.println("\nFinal states:");
        System.out.println(car);
        System.out.println(ev);
    }

    static void covariantReturnDemo() {
        System.out.println("\n=== Covariant Return (Builder) ===");

        // ExtendedBuilder chain — no cast needed because withName returns ExtendedBuilder
        ExtendedBuilder b = new ExtendedBuilder()
            .withName("Alice")
            .withAge(30)
            .withEmail("alice@example.com")
            .build();
        System.out.println(b);

        // If we use the parent reference, the covariant chain still works
        Builder base = new Builder().withName("Bob").withAge(25);
        System.out.println(base);
    }

    static void moneyDemo() {
        System.out.println("\n=== ImmutableMoney (final class) ===");

        ImmutableMoney salary    = ImmutableMoney.ofRupees(50_000, "INR");
        ImmutableMoney bonus     = ImmutableMoney.ofRupees(10_000, "INR");
        ImmutableMoney tax       = ImmutableMoney.ofRupees(5_000,  "INR");
        ImmutableMoney total     = salary.add(bonus);
        ImmutableMoney netSalary = total.subtract(tax);

        System.out.println("Salary:     " + salary);
        System.out.println("Bonus:      " + bonus);
        System.out.println("Total:      " + total);
        System.out.println("Net:        " + netSalary);
        System.out.println("Tax > bonus: " + tax.isGreaterThan(bonus));

        try {
            ImmutableMoney usd = ImmutableMoney.ofRupees(100, "USD");
            salary.add(usd);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        vehicleDemo();
        covariantReturnDemo();
        moneyDemo();
    }
}
