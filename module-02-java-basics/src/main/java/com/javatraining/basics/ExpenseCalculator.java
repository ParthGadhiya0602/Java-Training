package com.javatraining.basics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PRACTICAL EXERCISE: Expense Calculator
 *
 * This ties together every concept from this module:
 * - All primitive types with appropriate choices
 * - Integer vs floating-point division
 * - BigDecimal for money (critical production knowledge)
 * - Compound assignment operators
 * - Ternary operator
 * - Bitwise flags for status
 * - Type casting
 *
 * LESSON: For monetary calculations NEVER use double or float.
 * The imprecision of IEEE 754 causes real financial bugs.
 * Use BigDecimal with explicit scale and RoundingMode.
 */
public class ExpenseCalculator {

    // Expense status as bitmask flags — efficient storage, easy combination
    static final int STATUS_PENDING   = 0b0001; // 1
    static final int STATUS_APPROVED  = 0b0010; // 2
    static final int STATUS_REJECTED  = 0b0100; // 4
    static final int STATUS_REIMBURSED = 0b1000; // 8

    // Tax rate as BigDecimal to avoid floating-point errors
    static final BigDecimal GST_RATE = new BigDecimal("0.18"); // 18%

    // -------------------------------------------------------------------------
    // Why BigDecimal for money? Demonstration.
    // -------------------------------------------------------------------------
    static void whyNotDouble() {
        double price = 0.10;
        double tax   = 0.03;
        double total = price + tax;

        System.out.println("double: " + total);          // 0.13000000000000001 (WRONG!)
        System.out.println("double == 0.13: " + (total == 0.13)); // false

        // BigDecimal is precise — internally uses integer arithmetic with scale
        BigDecimal bdPrice = new BigDecimal("0.10");
        BigDecimal bdTax   = new BigDecimal("0.03");
        BigDecimal bdTotal = bdPrice.add(bdTax);

        System.out.println("BigDecimal: " + bdTotal);    // 0.13
        System.out.println("BD == 0.13: " + bdTotal.compareTo(new BigDecimal("0.13")) == 0); // true
    }

    // -------------------------------------------------------------------------
    // Calculate the total of an expense with GST and per-person split
    // -------------------------------------------------------------------------
    static BigDecimal[] calculateExpense(double amountRaw, int participants) {
        // Convert the raw double input to BigDecimal immediately at the boundary
        BigDecimal amount = BigDecimal.valueOf(amountRaw); // safer than new BigDecimal(double)

        // GST calculation — scale to 2 decimal places (paise precision)
        BigDecimal gst   = amount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = amount.add(gst).setScale(2, RoundingMode.HALF_UP);

        // Per-person split — integer division on BigDecimal also truncates,
        // so we use HALF_UP rounding
        BigDecimal perPerson = total.divide(
            BigDecimal.valueOf(participants), 2, RoundingMode.HALF_UP
        );

        return new BigDecimal[] { amount, gst, total, perPerson };
    }

    // -------------------------------------------------------------------------
    // Encode multiple statuses in a single int using bitwise OR
    // -------------------------------------------------------------------------
    static int createStatus(boolean approved, boolean reimbursed) {
        int status = STATUS_PENDING;
        if (approved)    status |= STATUS_APPROVED;
        if (reimbursed)  status |= STATUS_REIMBURSED;
        return status;
    }

    static String describeStatus(int status) {
        StringBuilder sb = new StringBuilder("[");
        if ((status & STATUS_PENDING)    != 0) sb.append("PENDING ");
        if ((status & STATUS_APPROVED)   != 0) sb.append("APPROVED ");
        if ((status & STATUS_REJECTED)   != 0) sb.append("REJECTED ");
        if ((status & STATUS_REIMBURSED) != 0) sb.append("REIMBURSED ");
        sb.append("]");
        return sb.toString().replace(" ]", "]");
    }

    // -------------------------------------------------------------------------
    // Category codes as bit flags — one int can hold multiple categories
    // -------------------------------------------------------------------------
    static final int CAT_TRAVEL  = 1 << 0; // 1
    static final int CAT_FOOD    = 1 << 1; // 2
    static final int CAT_LODGING = 1 << 2; // 4
    static final int CAT_EQUIP   = 1 << 3; // 8

    static String expenseType(int categories) {
        int count = Integer.bitCount(categories); // count set bits
        return count == 1 ? "Single-category"
             : count == 0 ? "Uncategorized"
             : "Multi-category";
    }

    // -------------------------------------------------------------------------
    // Monthly budget tracker — shows compound assignment in context
    // -------------------------------------------------------------------------
    static void trackMonthlyBudget() {
        BigDecimal budget    = new BigDecimal("50000.00"); // ₹50,000 monthly
        BigDecimal spent     = BigDecimal.ZERO;

        // Simulate weekly expenses
        double[] weeklyExpenses = { 8450.75, 12300.00, 9875.50, 11200.25 };
        int week = 0;

        for (double expense : weeklyExpenses) {
            week++;
            BigDecimal bdExpense = BigDecimal.valueOf(expense);
            spent = spent.add(bdExpense); // equivalent to spent += bdExpense

            BigDecimal remaining = budget.subtract(spent);
            int percentUsed = spent.multiply(BigDecimal.valueOf(100))
                                   .divide(budget, 0, RoundingMode.HALF_UP)
                                   .intValue();

            String alert = percentUsed >= 90 ? "CRITICAL"
                         : percentUsed >= 75 ? "WARNING"
                         : "OK";

            System.out.printf("Week %d | Spent: ₹%,.2f | Remaining: ₹%,.2f | %d%% | %s%n",
                week, spent, remaining, percentUsed, alert);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Why Not Double for Money ===");
        whyNotDouble();

        System.out.println("\n=== Expense Calculation ===");
        BigDecimal[] result = calculateExpense(5000.00, 4);
        System.out.printf("Base Amount : ₹%s%n", result[0]);
        System.out.printf("GST (18%%)  : ₹%s%n", result[1]);
        System.out.printf("Total       : ₹%s%n", result[2]);
        System.out.printf("Per Person  : ₹%s%n", result[3]);

        System.out.println("\n=== Status Flags ===");
        int status = createStatus(true, false);
        System.out.println("Status: " + describeStatus(status)); // [PENDING APPROVED]
        System.out.printf("Status as int: %d (binary: %s)%n", status, Integer.toBinaryString(status));

        System.out.println("\n=== Category Flags ===");
        int travelAndFood = CAT_TRAVEL | CAT_FOOD;
        System.out.println("Travel+Food: " + expenseType(travelAndFood)); // Multi-category
        System.out.println("Travel only: " + expenseType(CAT_TRAVEL));    // Single-category

        System.out.println("\n=== Monthly Budget Tracker ===");
        trackMonthlyBudget();
    }
}
