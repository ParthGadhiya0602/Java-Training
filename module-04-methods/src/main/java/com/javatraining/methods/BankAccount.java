package com.javatraining.methods;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PRACTICAL EXERCISE: BankAccount
 *
 * A realistic BankAccount class that demonstrates every concept from Module 04:
 *
 *   STATIC vs INSTANCE:
 *     - Static:  factory method open(), utility validate(), interestRate field
 *     - Instance: deposit(), withdraw(), transfer(), balance(), statement()
 *
 *   OVERLOADING:
 *     - deposit(double amount)
 *     - deposit(double amount, String description)
 *     - withdraw(double amount)
 *     - withdraw(double amount, String description)
 *
 *   VARARGS:
 *     - totalBalance(BankAccount... accounts) - sum multiple accounts
 *     - bulkDeposit(double amount, BankAccount... targets) - distribute evenly
 *
 *   RECURSION:
 *     - compoundInterest(BigDecimal principal, double rate, int periods)
 *       - uses recursion to apply interest period by period
 *
 *   PASS-BY-VALUE:
 *     - transfer(double amount, BankAccount target) shows that Java passes
 *       a copy of the reference - the objects ARE mutated (visible to caller),
 *       but reassigning 'target' inside the method would NOT affect the caller.
 *
 *   GUARD CLAUSES:
 *     - Every method validates its inputs before doing any real work.
 */
public class BankAccount {

    // -------------------------------------------------------------------------
    // Class-level (static) state - shared by ALL BankAccount instances
    // -------------------------------------------------------------------------
    private static int nextAccountNumber = 1000;
    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.06"); // 6% p.a.
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("500.00");

    // -------------------------------------------------------------------------
    // Instance state - each account has its own
    // -------------------------------------------------------------------------
    private final int    accountNumber;
    private final String holderName;
    private       BigDecimal balance;
    private final StringBuilder transactionLog = new StringBuilder();

    // -------------------------------------------------------------------------
    // Private constructor - creation is through the static factory method
    // -------------------------------------------------------------------------
    private BankAccount(String holderName, BigDecimal openingBalance) {
        this.accountNumber = nextAccountNumber++;
        this.holderName    = holderName;
        this.balance       = openingBalance;
        log("OPEN", openingBalance, "Account opened");
    }

    // =========================================================================
    // STATIC FACTORY METHOD - named constructor, communicates intent clearly
    // =========================================================================
    public static BankAccount open(String holderName, double openingBalance) {
        // Guard clauses first
        if (holderName == null || holderName.isBlank())
            throw new IllegalArgumentException("Holder name cannot be blank");
        if (openingBalance < 0)
            throw new IllegalArgumentException("Opening balance cannot be negative");

        BigDecimal bd = toBD(openingBalance);
        if (bd.compareTo(MINIMUM_BALANCE) < 0)
            throw new IllegalArgumentException(
                "Opening balance must be at least ₹" + MINIMUM_BALANCE);

        return new BankAccount(holderName.trim(), bd);
    }

    // =========================================================================
    // OVERLOADED deposit() - convenience versions
    // =========================================================================
    public void deposit(double amount) {
        deposit(amount, "Deposit");
    }

    public void deposit(double amount, String description) {
        // Guard clauses
        if (amount <= 0)
            throw new IllegalArgumentException("Deposit amount must be positive, got: " + amount);

        BigDecimal bd = toBD(amount);
        this.balance  = this.balance.add(bd);
        log("CR", bd, description);
    }

    // =========================================================================
    // OVERLOADED withdraw()
    // =========================================================================
    public void withdraw(double amount) {
        withdraw(amount, "Withdrawal");
    }

    public void withdraw(double amount, String description) {
        if (amount <= 0)
            throw new IllegalArgumentException("Withdrawal amount must be positive");

        BigDecimal bd = toBD(amount);
        BigDecimal afterWithdrawal = this.balance.subtract(bd);

        if (afterWithdrawal.compareTo(MINIMUM_BALANCE) < 0)
            throw new IllegalStateException(
                String.format("Insufficient funds. Balance: ₹%s, Requested: ₹%s, Min: ₹%s",
                    this.balance, bd, MINIMUM_BALANCE));

        this.balance = afterWithdrawal;
        log("DR", bd, description);
    }

    // =========================================================================
    // PASS-BY-VALUE demonstration inside transfer()
    //
    // Both 'this' and 'target' are object references - when passed to a method,
    // Java passes copies of those references. Both copies still point to the
    // same BankAccount objects on the heap, so mutations (withdraw/deposit)
    // ARE visible to the caller.
    //
    // If you did: target = new BankAccount(...) inside this method, the caller's
    // reference would NOT change - that's the pass-by-value guarantee.
    // =========================================================================
    public void transfer(double amount, BankAccount target) {
        if (target == null)
            throw new IllegalArgumentException("Target account cannot be null");
        if (target == this)
            throw new IllegalArgumentException("Cannot transfer to the same account");

        // These mutate the heap objects - visible to caller
        this.withdraw(amount, "Transfer to #" + target.accountNumber);
        target.deposit(amount, "Transfer from #" + this.accountNumber);
    }

    // =========================================================================
    // VARARGS: aggregate balance across multiple accounts
    // =========================================================================
    public static BigDecimal totalBalance(BankAccount... accounts) {
        if (accounts == null || accounts.length == 0)
            return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (BankAccount acc : accounts) {
            if (acc != null) total = total.add(acc.balance);
        }
        return total;
    }

    // VARARGS: distribute a fixed amount equally across multiple accounts
    public static void bulkDeposit(double amount, String description,
                                   BankAccount... targets) {
        if (targets == null || targets.length == 0) return;

        // Divide equally using BigDecimal to avoid rounding issues
        BigDecimal share = toBD(amount)
            .divide(BigDecimal.valueOf(targets.length), 2, RoundingMode.DOWN);
        // Any remainder due to rounding goes to the first account
        BigDecimal remainder = toBD(amount).subtract(
            share.multiply(BigDecimal.valueOf(targets.length)));

        for (int i = 0; i < targets.length; i++) {
            BigDecimal thisShare = (i == 0) ? share.add(remainder) : share;
            targets[i].deposit(thisShare.doubleValue(),
                description + " (bulk share " + (i + 1) + "/" + targets.length + ")");
        }
    }

    // =========================================================================
    // RECURSION: compound interest over N periods
    //
    //   compoundInterest(principal, rate, periods)
    //   = compoundInterest(principal * (1 + rate), rate, periods - 1)
    //
    // Each call applies ONE period of interest and delegates the remaining
    // periods to a recursive call. Base case: periods == 0, return principal.
    // =========================================================================
    public static BigDecimal compoundInterest(BigDecimal principal,
                                              double annualRate,
                                              int periods) {
        if (periods < 0)
            throw new IllegalArgumentException("Periods must be >= 0");
        if (periods == 0)
            return principal.setScale(2, RoundingMode.HALF_UP);  // base case

        BigDecimal growth = BigDecimal.ONE.add(BigDecimal.valueOf(annualRate));
        BigDecimal grown  = principal.multiply(growth).setScale(10, RoundingMode.HALF_UP);
        return compoundInterest(grown, annualRate, periods - 1); // recursive case
    }

    // =========================================================================
    // STATIC UTILITY - validate account number format
    // =========================================================================
    public static boolean isValidAccountNumber(int accountNumber) {
        return accountNumber >= 1000 && accountNumber < nextAccountNumber;
    }

    // =========================================================================
    // Helpers
    // =========================================================================
    private static BigDecimal toBD(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private void log(String type, BigDecimal amount, String description) {
        transactionLog.append(String.format("  %-4s %-9s  %s%n",
            type, "₹" + amount.toPlainString(), description));
    }

    public BigDecimal getBalance()    { return balance; }
    public int    getAccountNumber()  { return accountNumber; }
    public String getHolderName()     { return holderName; }

    public String statement() {
        return String.format(
            "Account #%d - %s%nBalance: ₹%s%nTransactions:%n%s",
            accountNumber, holderName, balance.toPlainString(), transactionLog);
    }

    // =========================================================================
    // Main - exercise every concept
    // =========================================================================
    public static void main(String[] args) {
        System.out.println("=== Static Factory: open() ===");
        BankAccount alice = BankAccount.open("Alice Sharma", 10_000);
        BankAccount bob   = BankAccount.open("Bob Patel",    5_000);
        System.out.printf("Account #%d opened for %s%n",
            alice.getAccountNumber(), alice.getHolderName());

        System.out.println("\n=== Overloaded deposit() ===");
        alice.deposit(2_500);                         // version 1: no description
        alice.deposit(1_000, "Freelance payment");    // version 2: with description
        System.out.println("Alice balance: ₹" + alice.getBalance());

        System.out.println("\n=== Overloaded withdraw() ===");
        alice.withdraw(500);                          // version 1
        alice.withdraw(300, "Electricity bill");      // version 2
        System.out.println("Alice balance: ₹" + alice.getBalance());

        System.out.println("\n=== Insufficient funds guard ===");
        try {
            alice.withdraw(50_000, "Lottery");
        } catch (IllegalStateException e) {
            System.out.println("Caught: " + e.getMessage());
        }

        System.out.println("\n=== transfer() - pass-by-value with objects ===");
        System.out.println("Before - Alice: ₹" + alice.getBalance()
            + "  Bob: ₹" + bob.getBalance());
        alice.transfer(3_000, bob);
        System.out.println("After  - Alice: ₹" + alice.getBalance()
            + "  Bob: ₹" + bob.getBalance());

        System.out.println("\n=== Varargs: totalBalance() ===");
        BankAccount carol = BankAccount.open("Carol Mehta", 8_000);
        BigDecimal total = BankAccount.totalBalance(alice, bob, carol);
        System.out.println("Total across 3 accounts: ₹" + total);

        System.out.println("\n=== Varargs: bulkDeposit() ===");
        BankAccount.bulkDeposit(1_500, "Annual bonus", alice, bob, carol);
        System.out.println("After bonus - Alice: ₹" + alice.getBalance()
            + "  Bob: ₹" + bob.getBalance()
            + "  Carol: ₹" + carol.getBalance());

        System.out.println("\n=== Recursion: compoundInterest() ===");
        BigDecimal principal = new BigDecimal("10000.00");
        for (int years = 1; years <= 5; years++) {
            BigDecimal maturity = BankAccount.compoundInterest(
                principal, 0.06, years);
            System.out.printf("₹10,000 at 6%% for %d year(s) = ₹%s%n",
                years, maturity);
        }

        System.out.println("\n=== Statement ===");
        System.out.println(alice.statement());
    }
}
