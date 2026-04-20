package com.javatraining.cleancode.smells;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Refactoring: Replace Data Clump with a Value Object.
 *
 * <p><strong>Smell — data clump:</strong> amount and currency always travel together
 * as separate parameters, easy to mix up or pass mismatched values.
 * <pre>
 *   double amount = 99.99;
 *   String currency = "USD";
 *   // Both must always be passed together — they're a clump
 *   void charge(double amount, String currency) { ... }
 *   void refund(double amount, String currency) { ... }
 * </pre>
 *
 * <p><strong>Fix:</strong> encapsulate into Money.
 * <pre>
 *   Money price = new Money(new BigDecimal("99.99"), "USD");
 *   void charge(Money amount) { ... }   // self-documenting, always consistent
 * </pre>
 *
 * <p>Also fixes the floating-point precision smell: use BigDecimal, not double.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        if (amount == null) throw new IllegalArgumentException("Amount must not be null");
        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("Currency must not be blank");
        if (amount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount must not be negative");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Result would be negative");
        return new Money(result, currency);
    }

    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!currency.equals(other.currency))
            throw new IllegalArgumentException(
                "Currency mismatch: " + currency + " vs " + other.currency);
    }

    @Override public String toString() { return currency + " " + amount.toPlainString(); }
}
