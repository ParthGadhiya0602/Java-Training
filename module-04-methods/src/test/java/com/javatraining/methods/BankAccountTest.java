package com.javatraining.methods;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BankAccount — practical exercise covering all Module 04 concepts")
class BankAccountTest {

    private BankAccount alice;
    private BankAccount bob;

    @BeforeEach
    void setUp() {
        alice = BankAccount.open("Alice", 10_000);
        bob   = BankAccount.open("Bob",   5_000);
    }

    // -------------------------------------------------------------------------
    // Static factory / guard clauses
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("open() — static factory and validation")
    class OpenTests {

        @Test
        @DisplayName("Blank name is rejected")
        void blankNameThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> BankAccount.open("  ", 1000));
        }

        @Test
        @DisplayName("Negative opening balance is rejected")
        void negativeBalanceThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> BankAccount.open("Carol", -100));
        }

        @Test
        @DisplayName("Opening balance below minimum is rejected")
        void belowMinimumThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> BankAccount.open("Dave", 100));
        }

        @Test
        @DisplayName("Each account gets a unique account number")
        void uniqueAccountNumbers() {
            assertNotEquals(alice.getAccountNumber(), bob.getAccountNumber());
        }
    }

    // -------------------------------------------------------------------------
    // Overloaded deposit / withdraw
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deposit() and withdraw() — overloaded methods")
    class DepositWithdrawTests {

        @Test
        @DisplayName("deposit(amount) increases balance correctly")
        void depositIncreasesBalance() {
            BigDecimal before = alice.getBalance();
            alice.deposit(2_000);
            assertEquals(before.add(new BigDecimal("2000.00")), alice.getBalance());
        }

        @Test
        @DisplayName("deposit(amount, description) also works")
        void depositWithDescriptionWorks() {
            alice.deposit(500, "Salary");
            assertEquals(new BigDecimal("10500.00"), alice.getBalance());
        }

        @Test
        @DisplayName("deposit with negative amount throws")
        void depositNegativeThrows() {
            assertThrows(IllegalArgumentException.class, () -> alice.deposit(-100));
        }

        @Test
        @DisplayName("withdraw(amount) decreases balance correctly")
        void withdrawDecreasesBalance() {
            alice.withdraw(2_000);
            assertEquals(new BigDecimal("8000.00"), alice.getBalance());
        }

        @Test
        @DisplayName("withdraw below minimum balance threshold throws")
        void withdrawBelowMinThrows() {
            assertThrows(IllegalStateException.class, () -> alice.withdraw(9_600));
        }
    }

    // -------------------------------------------------------------------------
    // transfer() — pass-by-value with objects
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("transfer() — pass-by-value demonstration")
    class TransferTests {

        @Test
        @DisplayName("Source balance decreases and target increases after transfer")
        void transferMutatesBothObjects() {
            alice.transfer(2_000, bob);
            assertEquals(new BigDecimal("8000.00"), alice.getBalance());
            assertEquals(new BigDecimal("7000.00"), bob.getBalance());
        }

        @Test
        @DisplayName("Transfer to null throws")
        void transferToNullThrows() {
            assertThrows(IllegalArgumentException.class, () -> alice.transfer(100, null));
        }

        @Test
        @DisplayName("Transfer to self throws")
        void transferToSelfThrows() {
            assertThrows(IllegalArgumentException.class, () -> alice.transfer(100, alice));
        }

        @Test
        @DisplayName("Transfer that would drop source below minimum throws")
        void transferBelowMinThrows() {
            assertThrows(IllegalStateException.class, () -> alice.transfer(9_600, bob));
        }
    }

    // -------------------------------------------------------------------------
    // Varargs
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("Varargs — totalBalance and bulkDeposit")
    class VarargsTests {

        @Test
        @DisplayName("totalBalance sums all account balances")
        void totalBalance() {
            BankAccount carol = BankAccount.open("Carol", 3_000);
            BigDecimal total = BankAccount.totalBalance(alice, bob, carol);
            assertEquals(new BigDecimal("18000.00"), total);
        }

        @Test
        @DisplayName("totalBalance with no accounts returns zero")
        void totalBalanceEmpty() {
            assertEquals(BigDecimal.ZERO, BankAccount.totalBalance());
        }

        @Test
        @DisplayName("bulkDeposit distributes amount across all targets")
        void bulkDeposit() {
            BankAccount carol = BankAccount.open("Carol", 5_000);
            // ₹1,500 split 3 ways: ₹500 each
            BankAccount.bulkDeposit(1_500, "Bonus", alice, bob, carol);

            assertEquals(new BigDecimal("10500.00"), alice.getBalance());
            assertEquals(new BigDecimal("5500.00"),  bob.getBalance());
            assertEquals(new BigDecimal("5500.00"),  carol.getBalance());
        }
    }

    // -------------------------------------------------------------------------
    // Recursion: compoundInterest
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("compoundInterest() — recursive calculation")
    class CompoundInterestTests {

        @Test
        @DisplayName("0 periods returns original principal")
        void zeroPeriods() {
            BigDecimal principal = new BigDecimal("10000.00");
            assertEquals(principal, BankAccount.compoundInterest(principal, 0.06, 0));
        }

        @Test
        @DisplayName("1 period at 6% gives 10600.00")
        void onePeriod() {
            BigDecimal result = BankAccount.compoundInterest(
                new BigDecimal("10000.00"), 0.06, 1);
            assertEquals(new BigDecimal("10600.00"), result);
        }

        @Test
        @DisplayName("3 periods compound correctly")
        void threePeriods() {
            // 10000 * 1.06^3 = 11910.16
            BigDecimal result = BankAccount.compoundInterest(
                new BigDecimal("10000.00"), 0.06, 3);
            assertEquals(new BigDecimal("11910.16"), result);
        }

        @Test
        @DisplayName("Negative periods throws")
        void negativePeriodsThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> BankAccount.compoundInterest(new BigDecimal("1000"), 0.06, -1));
        }
    }
}
