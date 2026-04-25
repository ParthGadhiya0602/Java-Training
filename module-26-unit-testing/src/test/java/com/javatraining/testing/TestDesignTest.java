package com.javatraining.testing;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates test design principles applied to BankAccount.
 *
 * Principles shown:
 *   AAA            - Arrange / Act / Assert: three clearly separated phases per test
 *   Isolation       - @BeforeEach creates a fresh instance; no shared mutable state
 *   Naming          - method names read as sentences describing observable behaviour
 *   Single concept  - each test verifies exactly one thing
 *   Edge cases      - zero amount, exact-balance withdrawal, empty history
 *   Boundaries      - very first and last valid/invalid values
 *   Exception detail- verify exception type AND message AND field values
 *   State integrity - failed operations must leave the object unchanged
 *   Immutability    - returned views must not allow mutation
 *   F.I.R.S.T.     - Fast, Isolated, Repeatable, Self-validating, Timely
 */
@DisplayName("BankAccount - test design principles")
class TestDesignTest {

    // Named constant - avoids magic numbers in assertions
    static final double INITIAL_BALANCE = 500.0;

    BankAccount account;

    /**
     * @BeforeEach ensures every test starts with a brand-new account.
     * No test can affect another's starting state (isolation).
     */
    @BeforeEach
    void freshAccount() {
        account = new BankAccount("acc-test", INITIAL_BALANCE);
    }

    // ── AAA: Arrange / Act / Assert clearly separated ─────────────────────────

    @Test
    @DisplayName("Deposit increases balance by the deposited amount")
    void deposit_increases_balance_by_exact_amount() {
        // Arrange
        double depositAmount = 200.0;

        // Act
        account.deposit(depositAmount);

        // Assert
        assertEquals(INITIAL_BALANCE + depositAmount, account.balance());
    }

    @Test
    @DisplayName("Withdraw decreases balance by the withdrawn amount")
    void withdraw_decreases_balance_by_exact_amount() {
        // Arrange
        double withdrawAmount = 150.0;

        // Act
        account.withdraw(withdrawAmount);

        // Assert
        assertEquals(INITIAL_BALANCE - withdrawAmount, account.balance());
    }

    // ── assertAll: related assertions on the same result ─────────────────────

    @Test
    void deposit_records_transaction_with_all_correct_fields() {
        account.deposit(100.0);

        BankAccount.Transaction tx = account.transactions().get(0);
        assertAll("transaction",
            () -> assertEquals("DEPOSIT", tx.type()),
            () -> assertEquals(100.0,     tx.amount()),
            () -> assertEquals(600.0,     tx.balanceAfter()),
            () -> assertNotNull(tx.timestamp())
        );
    }

    @Test
    void withdrawal_records_transaction_with_all_correct_fields() {
        account.withdraw(200.0);

        BankAccount.Transaction tx = account.transactions().get(0);
        assertAll("transaction",
            () -> assertEquals("WITHDRAWAL", tx.type()),
            () -> assertEquals(200.0,        tx.amount()),
            () -> assertEquals(300.0,        tx.balanceAfter())
        );
    }

    // ── Boundary / edge cases ─────────────────────────────────────────────────

    @Test
    @DisplayName("Withdrawing exactly the full balance leaves zero")
    void withdraw_entire_balance_results_in_zero() {
        account.withdraw(INITIAL_BALANCE);
        assertEquals(0.0, account.balance());
    }

    @Test
    @DisplayName("Multiple deposits accumulate correctly")
    void multiple_deposits_accumulate() {
        account.deposit(100.0);
        account.deposit(200.0);
        account.deposit(50.0);
        assertEquals(850.0, account.balance());
    }

    @Test
    void transaction_count_matches_number_of_operations() {
        account.deposit(100.0);
        account.withdraw(50.0);
        account.deposit(25.0);
        assertEquals(3, account.transactions().size());
    }

    // ── Exception: type, message, and payload ────────────────────────────────

    @Test
    @DisplayName("Overdraft throws InsufficientFundsException with correct fields")
    void overdraft_throws_with_full_details() {
        double overdraft = INITIAL_BALANCE + 100.0;

        BankAccount.InsufficientFundsException ex = assertThrows(
            BankAccount.InsufficientFundsException.class,
            () -> account.withdraw(overdraft));

        assertAll("exception",
            () -> assertEquals(overdraft,       ex.requested(), 0.001),
            () -> assertEquals(INITIAL_BALANCE, ex.available(), 0.001),
            () -> assertTrue(ex.getMessage().contains("Insufficient funds"))
        );
    }

    // ── State integrity: failed operations must not change state ─────────────

    @Test
    @DisplayName("Balance and history unchanged after failed withdrawal")
    void failed_withdrawal_leaves_state_unchanged() {
        assertThrows(BankAccount.InsufficientFundsException.class,
            () -> account.withdraw(INITIAL_BALANCE + 1));

        assertAll("unchanged state",
            () -> assertEquals(INITIAL_BALANCE, account.balance(),
                    "balance should not change"),
            () -> assertTrue(account.transactions().isEmpty(),
                    "no transaction should be recorded")
        );
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "deposit({0}) throws IllegalArgumentException")
    @ValueSource(doubles = {0.0, -1.0, -100.0})
    void deposit_non_positive_amount_throws(double amount) {
        assertThrows(IllegalArgumentException.class, () -> account.deposit(amount));
    }

    @ParameterizedTest(name = "withdraw({0}) throws IllegalArgumentException")
    @ValueSource(doubles = {0.0, -1.0, -50.0})
    void withdraw_non_positive_amount_throws(double amount) {
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(amount));
    }

    @Test
    void constructor_rejects_negative_initial_balance() {
        assertThrows(IllegalArgumentException.class,
            () -> new BankAccount("bad-acc", -100.0));
    }

    // ── Parameterized deposit / withdraw cycles ───────────────────────────────

    @ParameterizedTest(name = "deposit {0}, withdraw {1} → balance {2}")
    @CsvSource({
        "200.0, 100.0, 600.0",
        "50.0,  50.0,  500.0",
        "300.0, 300.0, 500.0"
    })
    void deposit_then_withdraw(double dep, double wdr, double expected) {
        account.deposit(dep);
        account.withdraw(wdr);
        assertEquals(expected, account.balance(), 0.001);
    }

    // ── Isolation checks ─────────────────────────────────────────────────────

    @Test
    void account_starts_with_correct_id() {
        assertEquals("acc-test", account.accountId());
    }

    @Test
    void fresh_account_has_no_transactions() {
        // @BeforeEach creates a new account; any transactions from other tests
        // are invisible here because each test has its own instance.
        assertTrue(account.transactions().isEmpty());
    }

    @Test
    void fresh_account_balance_equals_initial() {
        assertEquals(INITIAL_BALANCE, account.balance());
    }

    // ── Immutability of returned views ────────────────────────────────────────

    @Test
    void transaction_list_is_unmodifiable() {
        account.deposit(100.0);
        assertThrows(UnsupportedOperationException.class,
            () -> account.transactions().clear());
    }
}
