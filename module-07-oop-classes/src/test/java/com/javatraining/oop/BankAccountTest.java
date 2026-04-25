package com.javatraining.oop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BankAccountTest {

    // -----------------------------------------------------------------------
    // Opening accounts
    // -----------------------------------------------------------------------
    @Test
    void open_savings_with_valid_deposit() {
        BankAccount acc = BankAccount.openSavings("Alice", 1_000);
        assertEquals(1_000.0, acc.balance(), 1e-9);
        assertEquals("Alice",   acc.holderName());
        assertEquals(BankAccount.AccountType.SAVINGS, acc.type());
    }

    @Test
    void open_current_with_valid_deposit() {
        BankAccount acc = BankAccount.openCurrent("Bob", 5_000);
        assertEquals(BankAccount.AccountType.CURRENT, acc.type());
    }

    @Test
    void open_with_below_minimum_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> BankAccount.openSavings("X", 100));
    }

    @Test
    void open_with_blank_name_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> BankAccount.openSavings("  ", 1_000));
    }

    @Test
    void each_account_gets_unique_number() {
        BankAccount a = BankAccount.openSavings("A", 1_000);
        BankAccount b = BankAccount.openSavings("B", 1_000);
        assertNotEquals(a.accountNumber(), b.accountNumber());
    }

    @Test
    void opening_creates_one_deposit_transaction() {
        BankAccount acc = BankAccount.openSavings("Alice", 2_000);
        assertEquals(1, acc.history().size());
        assertEquals(BankAccount.TxType.DEPOSIT, acc.history().get(0).type());
    }

    // -----------------------------------------------------------------------
    // Deposit
    // -----------------------------------------------------------------------
    @Nested
    class Deposit {
        BankAccount acc;

        @BeforeEach
        void setup() { acc = BankAccount.openSavings("Alice", 1_000); }

        @Test
        void deposit_increases_balance() {
            acc.deposit(500);
            assertEquals(1_500.0, acc.balance(), 1e-9);
        }

        @Test
        void deposit_with_note_records_in_history() {
            acc.deposit(300, "Bonus");
            var last = acc.lastTransaction().orElseThrow();
            assertEquals(BankAccount.TxType.DEPOSIT, last.type());
            assertEquals(300.0, last.amount(), 1e-9);
        }

        @Test
        void zero_deposit_throws() {
            assertThrows(IllegalArgumentException.class, () -> acc.deposit(0));
        }

        @Test
        void negative_deposit_throws() {
            assertThrows(IllegalArgumentException.class, () -> acc.deposit(-100));
        }
    }

    // -----------------------------------------------------------------------
    // Withdrawal
    // -----------------------------------------------------------------------
    @Nested
    class Withdrawal {
        BankAccount acc;

        @BeforeEach
        void setup() { acc = BankAccount.openSavings("Bob", 2_000); }

        @Test
        void withdraw_decreases_balance() {
            acc.withdraw(500);
            assertEquals(1_500.0, acc.balance(), 1e-9);
        }

        @Test
        void withdraw_enforces_minimum_balance() {
            // 2000 - 1600 = 400 < 500 minimum
            assertThrows(IllegalStateException.class, () -> acc.withdraw(1_600));
            assertEquals(2_000.0, acc.balance(), 1e-9); // unchanged
        }

        @Test
        void withdraw_exactly_to_minimum_allowed() {
            acc.withdraw(1_500); // leaves exactly ₹500
            assertEquals(500.0, acc.balance(), 1e-9);
        }

        @Test
        void zero_withdrawal_throws() {
            assertThrows(IllegalArgumentException.class, () -> acc.withdraw(0));
        }
    }

    // -----------------------------------------------------------------------
    // Transfer
    // -----------------------------------------------------------------------
    @Nested
    class Transfer {
        BankAccount sender, receiver;

        @BeforeEach
        void setup() {
            sender   = BankAccount.openSavings("Sender",   10_000);
            receiver = BankAccount.openSavings("Receiver", 2_000);
        }

        @Test
        void transfer_moves_money_between_accounts() {
            BankAccount.transfer(sender, receiver, 3_000);
            assertEquals(7_000.0,  sender.balance(),   1e-9);
            assertEquals(5_000.0,  receiver.balance(), 1e-9);
        }

        @Test
        void transfer_records_tx_on_both_accounts() {
            BankAccount.transfer(sender, receiver, 1_000);
            assertEquals(BankAccount.TxType.TRANSFER_OUT,
                sender.lastTransaction().orElseThrow().type());
            assertEquals(BankAccount.TxType.TRANSFER_IN,
                receiver.lastTransaction().orElseThrow().type());
        }

        @Test
        void transfer_that_violates_min_balance_throws() {
            // sender has 10000, minimum 500 - can transfer at most 9500
            assertThrows(IllegalStateException.class,
                () -> BankAccount.transfer(sender, receiver, 9_600));
        }

        @Test
        void zero_transfer_throws() {
            assertThrows(IllegalArgumentException.class,
                () -> BankAccount.transfer(sender, receiver, 0));
        }
    }

    // -----------------------------------------------------------------------
    // equals / hashCode / defensive copy
    // -----------------------------------------------------------------------
    @Test
    void same_account_is_equal_to_itself() {
        BankAccount acc = BankAccount.openSavings("Alice", 1_000);
        assertEquals(acc, acc);
    }

    @Test
    void two_accounts_for_same_person_are_not_equal() {
        BankAccount a = BankAccount.openSavings("Alice", 1_000);
        BankAccount b = BankAccount.openSavings("Alice", 1_000);
        assertNotEquals(a, b);
    }

    @Test
    void equal_accounts_have_equal_hashcodes() {
        BankAccount acc = BankAccount.openSavings("Alice", 1_000);
        // Only one instance can equal itself; verify self-consistency
        assertEquals(acc.hashCode(), acc.hashCode());
    }

    @Test
    void history_is_unmodifiable() {
        BankAccount acc = BankAccount.openSavings("Alice", 1_000);
        List<BankAccount.Transaction> hist = acc.history();
        assertThrows(UnsupportedOperationException.class, () -> hist.add(null));
    }

    @Test
    void last_transaction_empty_never_happens_after_open() {
        // opening always records a transaction
        BankAccount acc = BankAccount.openSavings("Alice", 1_000);
        assertTrue(acc.lastTransaction().isPresent());
    }

    // -----------------------------------------------------------------------
    // Transaction record
    // -----------------------------------------------------------------------
    @Test
    void transaction_zero_amount_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new BankAccount.Transaction(
                BankAccount.TxType.DEPOSIT, 0, 1000, "test"));
    }

    @Test
    void transaction_negative_amount_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> new BankAccount.Transaction(
                BankAccount.TxType.DEPOSIT, -50, 1000, "test"));
    }
}
