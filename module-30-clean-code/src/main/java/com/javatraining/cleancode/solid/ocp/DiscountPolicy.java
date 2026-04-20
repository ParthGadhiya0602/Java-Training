package com.javatraining.cleancode.solid.ocp;

/**
 * OCP — Open/Closed Principle.
 *
 * <p>The abstraction that makes the system open for extension (add new discount
 * types by adding new classes) without modifying existing code.
 *
 * <p><strong>Before (OCP violation):</strong>
 * <pre>
 *   double calculate(double total, String type) {
 *       if (type.equals("VIP"))      return total * 0.20;
 *       if (type.equals("REGULAR"))  return total * 0.10;
 *       // Adding "SEASONAL" requires modifying this method — OCP violation
 *       return 0;
 *   }
 * </pre>
 *
 * <p><strong>After (OCP compliant):</strong>
 * Adding a new discount type = adding a new class, zero changes to existing code.
 */
public interface DiscountPolicy {
    /** Returns the discount amount (not percentage) for the given order total. */
    double calculate(double orderTotal);

    /** Human-readable name for logging/display. */
    String name();
}
