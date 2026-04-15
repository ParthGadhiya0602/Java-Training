package com.javatraining.encapsulation;

/**
 * TOPIC: Access modifiers and encapsulation fundamentals
 *
 * The four levels, most to least restrictive:
 *   private          — only within the same class
 *   (package-private)— within the same package (no keyword)
 *   protected        — same package + subclasses
 *   public           — everywhere
 *
 * Design rule: use the MOST restrictive modifier that still lets
 * the class do its job.  Private first; open up only when needed.
 *
 * Invariant: a condition that must ALWAYS be true for an object to
 * be in a valid state.  Enforce it at every mutation point.
 */
public class AccessModifiers {

    // -------------------------------------------------------------------------
    // 1. BankAccount — textbook private fields + public accessors
    //    Invariants: balance >= 0, limit > 0, withdrawals respect limit
    // -------------------------------------------------------------------------
    static class BankAccount {

        // private — not even subclasses should touch these directly
        private final String accountId;
        private final String owner;
        private       double balance;
        private       double dailyWithdrawalLimit;
        private       double withdrawnToday;

        // package-private constructor — only this package can instantiate
        BankAccount(String accountId, String owner,
                    double initialBalance, double dailyLimit) {
            requireNonBlank(accountId, "accountId");
            requireNonBlank(owner,     "owner");
            requirePositive(initialBalance, "initialBalance");
            requirePositive(dailyLimit,     "dailyLimit");

            this.accountId            = accountId;
            this.owner                = owner;
            this.balance              = initialBalance;
            this.dailyWithdrawalLimit = dailyLimit;
            this.withdrawnToday       = 0;
        }

        // public read-only accessors — callers can SEE state but not set it
        public String accountId()          { return accountId; }
        public String owner()              { return owner; }
        public double balance()            { return balance; }
        public double dailyLimit()         { return dailyWithdrawalLimit; }
        public double withdrawnToday()     { return withdrawnToday; }
        public double remainingLimit()     { return dailyWithdrawalLimit - withdrawnToday; }

        // public mutation methods — each enforces invariants before changing state
        public void deposit(double amount) {
            requirePositive(amount, "deposit amount");
            balance += amount;
        }

        public void withdraw(double amount) {
            requirePositive(amount, "withdrawal amount");
            if (amount > balance)
                throw new IllegalStateException("Insufficient balance");
            if (amount > remainingLimit())
                throw new IllegalStateException(
                    "Daily limit exceeded. Remaining: " + remainingLimit());
            balance       -= amount;
            withdrawnToday += amount;
        }

        public void setDailyLimit(double newLimit) {
            requirePositive(newLimit, "daily limit");
            this.dailyWithdrawalLimit = newLimit;
        }

        // package-private — only tests/same-package can reset daily counter
        void resetDailyWithdrawals() { withdrawnToday = 0; }

        // private helpers — internal to this class only
        private static void requirePositive(double v, String name) {
            if (v <= 0) throw new IllegalArgumentException(name + " must be > 0, got: " + v);
        }

        private static void requireNonBlank(String s, String name) {
            if (s == null || s.isBlank())
                throw new IllegalArgumentException(name + " must not be blank");
        }

        @Override
        public String toString() {
            return String.format("BankAccount{id=%s, owner=%s, balance=%.2f, limit=%.2f}",
                accountId, owner, balance, dailyWithdrawalLimit);
        }
    }

    // -------------------------------------------------------------------------
    // 2. Temperature hierarchy — protected for subclass extension
    //    Invariant: kelvin representation is always >= 0
    // -------------------------------------------------------------------------
    static class Temperature {
        // protected — subclasses read it; package sees it; outside cannot
        protected final double kelvin;

        // public factory — named constructor
        public static Temperature ofCelsius(double c) {
            if (c < -273.15)
                throw new IllegalArgumentException(
                    "Below absolute zero: " + c + "°C");
            return new Temperature(c + 273.15);
        }

        public static Temperature ofKelvin(double k) {
            if (k < 0)
                throw new IllegalArgumentException(
                    "Kelvin cannot be negative: " + k);
            return new Temperature(k);
        }

        // private constructor — forces use of factories
        private Temperature(double kelvin) { this.kelvin = kelvin; }

        public double toCelsius()    { return kelvin - 273.15; }
        public double toFahrenheit() { return toCelsius() * 9.0 / 5.0 + 32; }
        public double toKelvin()     { return kelvin; }

        // protected — subclasses may use in overrides
        protected String unitSymbol() { return "K"; }

        @Override
        public String toString() {
            return String.format("%.2f%s", kelvin, unitSymbol());
        }
    }

    // Subclass accesses protected field and method
    static class DisplayTemperature extends Temperature {

        private final String preferredUnit;

        DisplayTemperature(Temperature base, String unit) {
            super(base.kelvin);   // access to protected field via super()
            this.preferredUnit = unit.toUpperCase();
        }

        // We can read protected kelvin directly here
        @Override
        protected String unitSymbol() {
            return switch (preferredUnit) {
                case "C" -> "°C";
                case "F" -> "°F";
                default  -> "K";
            };
        }

        @Override
        public String toString() {
            return switch (preferredUnit) {
                case "C" -> String.format("%.2f%s", toCelsius(),    unitSymbol());
                case "F" -> String.format("%.2f%s", toFahrenheit(), unitSymbol());
                default  -> String.format("%.2f%s", kelvin,         unitSymbol());
            };
        }
    }

    // -------------------------------------------------------------------------
    // 3. Counter — package-private class (no modifier on class itself)
    //    Only accessible within this package — not part of the public API.
    // -------------------------------------------------------------------------
    static class PackageCounter {
        private int count = 0;

        // package-private — internal use only
        void increment()       { count++;           }
        void increment(int by) { count += by;       }
        void reset()           { count  = 0;        }
        int  count()           { return count;      }
        boolean isZero()       { return count == 0; }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void bankAccountDemo() {
        System.out.println("=== BankAccount (private fields, public interface) ===");

        BankAccount acc = new BankAccount("ACC001", "Alice", 10_000, 2_000);
        System.out.println(acc);

        acc.deposit(5_000);
        System.out.printf("After deposit:    balance=%.0f%n", acc.balance());

        acc.withdraw(1_500);
        System.out.printf("After withdraw:   balance=%.0f  withdrawnToday=%.0f%n",
            acc.balance(), acc.withdrawnToday());

        System.out.printf("Remaining limit:  %.0f%n", acc.remainingLimit());

        // Invariant violations
        try { acc.withdraw(5_000); }  // over daily limit
        catch (IllegalStateException e) { System.out.println("Caught: " + e.getMessage()); }

        try { new BankAccount("", "Bob", 1_000, 500); }
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }

        try { acc.deposit(-100); }
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void temperatureDemo() {
        System.out.println("\n=== Temperature (protected for subclass) ===");

        Temperature boiling = Temperature.ofCelsius(100);
        Temperature body    = Temperature.ofCelsius(37);

        System.out.println("Boiling (raw):  " + boiling);
        System.out.println("Body temp (raw):" + body);

        DisplayTemperature dispC = new DisplayTemperature(boiling, "C");
        DisplayTemperature dispF = new DisplayTemperature(boiling, "F");
        System.out.println("Boiling in °C:  " + dispC);
        System.out.println("Boiling in °F:  " + dispF);

        try { Temperature.ofCelsius(-300); }
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        bankAccountDemo();
        temperatureDemo();
    }
}
