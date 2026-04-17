package com.javatraining.testing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple bank account — used as the subject-under-test for test design demos.
 * Demonstrates stateful objects with validation, custom exceptions, and immutable
 * transaction history; all ideal properties for exercising good test design.
 */
public class BankAccount {

    // ── Nested types ──────────────────────────────────────────────────────────

    /** Immutable snapshot of a single debit or credit. */
    public record Transaction(String type, double amount, double balanceAfter, Instant timestamp) {}

    /** Thrown when a withdrawal exceeds the available balance. */
    public static class InsufficientFundsException extends RuntimeException {
        private final double requested;
        private final double available;

        public InsufficientFundsException(double requested, double available) {
            super(String.format("Insufficient funds: requested %.2f but only %.2f available",
                requested, available));
            this.requested = requested;
            this.available = available;
        }

        public double requested() { return requested; }
        public double available() { return available; }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final String accountId;
    private double balance;
    private final List<Transaction> transactions = new ArrayList<>();

    public BankAccount(String accountId, double initialBalance) {
        if (initialBalance < 0)
            throw new IllegalArgumentException("Initial balance cannot be negative");
        this.accountId = accountId;
        this.balance   = initialBalance;
    }

    // ── Operations ────────────────────────────────────────────────────────────

    /**
     * Credits the account.
     * @throws IllegalArgumentException if {@code amount <= 0}
     */
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, balance, Instant.now()));
    }

    /**
     * Debits the account.
     * @throws IllegalArgumentException     if {@code amount <= 0}
     * @throws InsufficientFundsException   if {@code amount > balance}
     */
    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive");
        if (amount > balance) throw new InsufficientFundsException(amount, balance);
        balance -= amount;
        transactions.add(new Transaction("WITHDRAWAL", amount, balance, Instant.now()));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public double       balance()      { return balance; }
    public String       accountId()    { return accountId; }
    public List<Transaction> transactions() {
        return Collections.unmodifiableList(transactions);
    }
}
