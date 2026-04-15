package com.javatraining.oop;

import java.time.LocalDateTime;
import java.util.*;

/**
 * TOPIC: Full class design — integrating every concept from the module
 *
 * Demonstrates:
 *   • Static factory instead of public constructor
 *   • Constructor chaining via this(...)
 *   • Encapsulated mutable state with invariant enforcement
 *   • Correct equals/hashCode on account number (natural identity)
 *   • toString for logging/debugging
 *   • Static members: nextId counter, MIN_BALANCE
 *   • Defensive copy for returning mutable collections (transaction history)
 *   • Value object inner record (Transaction)
 *   • Object graph: Account references List<Transaction>
 */
public class BankAccount {

    // -------------------------------------------------------------------------
    // Transaction — immutable value object (record)
    // -------------------------------------------------------------------------
    enum TxType { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT }

    record Transaction(TxType type, double amount, double balanceAfter,
                       String description, LocalDateTime at) {
        Transaction {
            if (amount <= 0) throw new IllegalArgumentException("amount must be > 0");
        }

        // Convenience constructor: uses current time
        Transaction(TxType type, double amount, double balanceAfter, String description) {
            this(type, amount, balanceAfter, description, LocalDateTime.now());
        }

        @Override
        public String toString() {
            return String.format("  [%s] %-14s ₹%9.2f  balance: ₹%9.2f  — %s",
                at.toLocalTime(), type, amount, balanceAfter, description);
        }
    }

    // -------------------------------------------------------------------------
    // Account type
    // -------------------------------------------------------------------------
    enum AccountType { SAVINGS, CURRENT, FIXED_DEPOSIT }

    // -------------------------------------------------------------------------
    // BankAccount
    // -------------------------------------------------------------------------
    private static int nextId = 1000;               // static: shared, increments
    private static final double MIN_BALANCE = 500.0; // static constant

    private final String       accountNumber;
    private final String       holderName;
    private final AccountType  type;
    private       double       balance;
    private final List<Transaction> history = new ArrayList<>();

    // private constructor — creation goes through static factories
    private BankAccount(String holderName, AccountType type, double initialDeposit) {
        if (holderName == null || holderName.isBlank())
            throw new IllegalArgumentException("holder name is required");
        if (initialDeposit < MIN_BALANCE)
            throw new IllegalArgumentException(
                "Initial deposit must be at least ₹" + MIN_BALANCE);

        this.accountNumber = "ACC" + (++nextId);
        this.holderName    = holderName;
        this.type          = type;
        this.balance       = initialDeposit;
        record(new Transaction(TxType.DEPOSIT, initialDeposit, balance, "Account opening"));
    }

    // -------------------------------------------------------------------------
    // Static factories — named, self-documenting
    // -------------------------------------------------------------------------
    public static BankAccount openSavings(String holder, double initialDeposit) {
        return new BankAccount(holder, AccountType.SAVINGS, initialDeposit);
    }

    public static BankAccount openCurrent(String holder, double initialDeposit) {
        return new BankAccount(holder, AccountType.CURRENT, initialDeposit);
    }

    // -------------------------------------------------------------------------
    // Core operations
    // -------------------------------------------------------------------------
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");
        balance += amount;
        record(new Transaction(TxType.DEPOSIT, amount, balance, "Cash deposit"));
    }

    public void deposit(double amount, String note) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive");
        balance += amount;
        record(new Transaction(TxType.DEPOSIT, amount, balance, note));
    }

    public void withdraw(double amount) {
        withdraw(amount, "Cash withdrawal");
    }

    public void withdraw(double amount, String note) {
        if (amount <= 0)
            throw new IllegalArgumentException("Withdrawal must be positive");
        if (balance - amount < MIN_BALANCE)
            throw new IllegalStateException(
                "Insufficient balance. Minimum balance ₹" + MIN_BALANCE + " must be maintained.");
        balance -= amount;
        record(new Transaction(TxType.WITHDRAWAL, amount, balance, note));
    }

    /**
     * Transfer — shows pass-by-value with objects: we mutate the objects, not
     * the references, so both accounts are modified correctly.
     */
    public static void transfer(BankAccount from, BankAccount to, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive");
        // withdraw from sender
        from.withdraw(amount, "Transfer to " + to.accountNumber);
        // deposit to receiver (direct field manipulation within same class)
        to.balance += amount;
        to.record(new Transaction(TxType.TRANSFER_IN, amount, to.balance,
                                  "Transfer from " + from.accountNumber));
        // patch sender's last tx type
        from.history.set(from.history.size() - 1,
            new Transaction(TxType.TRANSFER_OUT, amount, from.balance,
                            "Transfer to " + to.accountNumber));
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------
    public double balance()           { return balance; }
    public String accountNumber()     { return accountNumber; }
    public String holderName()        { return holderName; }
    public AccountType type()         { return type; }

    /** Defensive copy — caller cannot modify internal history */
    public List<Transaction> history() {
        return Collections.unmodifiableList(history);
    }

    public Optional<Transaction> lastTransaction() {
        return history.isEmpty()
            ? Optional.empty()
            : Optional.of(history.get(history.size() - 1));
    }

    // -------------------------------------------------------------------------
    // equals / hashCode — identity is the account number (natural key)
    // Two accounts opened for the same person are still different accounts.
    // -------------------------------------------------------------------------
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankAccount other)) return false;
        return accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }

    @Override
    public String toString() {
        return String.format("BankAccount{%s | %s | %s | balance=₹%.2f | txns=%d}",
            accountNumber, holderName, type, balance, history.size());
    }

    // -------------------------------------------------------------------------
    // Private helper
    // -------------------------------------------------------------------------
    private void record(Transaction tx) {
        history.add(tx);
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void basicOpsDemo() {
        System.out.println("=== Basic Operations ===");

        BankAccount alice = BankAccount.openSavings("Alice Sharma", 10_000);
        BankAccount bob   = BankAccount.openCurrent("Bob Patel",    5_000);

        System.out.println("Opened: " + alice);
        System.out.println("Opened: " + bob);

        alice.deposit(2_500, "Salary credit");
        alice.withdraw(1_000, "Grocery");
        alice.deposit(500);

        System.out.println("\nAlice after ops: " + alice);
        System.out.println("Alice history:");
        alice.history().forEach(System.out::println);
    }

    static void transferDemo() {
        System.out.println("\n=== Transfer (pass-by-value with objects) ===");

        BankAccount sender   = BankAccount.openSavings("Sender",   20_000);
        BankAccount receiver = BankAccount.openSavings("Receiver", 5_000);

        System.out.printf("Before: sender=₹%.0f  receiver=₹%.0f%n",
            sender.balance(), receiver.balance());

        BankAccount.transfer(sender, receiver, 7_500);

        System.out.printf("After:  sender=₹%.0f  receiver=₹%.0f%n",
            sender.balance(), receiver.balance());

        System.out.println("Sender last tx:   " + sender.lastTransaction().orElseThrow());
        System.out.println("Receiver last tx: " + receiver.lastTransaction().orElseThrow());
    }

    static void invariantDemo() {
        System.out.println("\n=== Invariant Enforcement ===");

        BankAccount acc = BankAccount.openSavings("Test", 1_000);

        try { acc.withdraw(600); }  // would leave ₹400 < ₹500 minimum
        catch (IllegalStateException e) { System.out.println("Caught: " + e.getMessage()); }

        try { BankAccount.openSavings("X", 100); }  // below minimum
        catch (IllegalArgumentException e) { System.out.println("Caught: " + e.getMessage()); }
    }

    static void equalsDemo() {
        System.out.println("\n=== equals / hashCode on account number ===");

        BankAccount a1 = BankAccount.openSavings("Alice", 1_000);
        BankAccount a2 = BankAccount.openSavings("Alice", 1_000); // same holder, different acct

        System.out.println("a1: " + a1.accountNumber());
        System.out.println("a2: " + a2.accountNumber());
        System.out.println("a1.equals(a2): " + a1.equals(a2));   // false — different account numbers
        System.out.println("a1.equals(a1): " + a1.equals(a1));   // true

        Set<BankAccount> accounts = new HashSet<>();
        accounts.add(a1);
        accounts.add(a1); // duplicate
        System.out.println("Set size (should be 1): " + accounts.size());

        // Defensive copy test
        List<Transaction> hist = a1.history();
        try {
            hist.add(null);  // should throw — unmodifiable list
        } catch (UnsupportedOperationException e) {
            System.out.println("Defensive copy works — history is unmodifiable");
        }
    }

    public static void main(String[] args) {
        basicOpsDemo();
        transferDemo();
        invariantDemo();
        equalsDemo();
    }
}
