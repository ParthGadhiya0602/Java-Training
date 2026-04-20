package com.javatraining.cleancode.solid.ocp;

/**
 * Concrete discount policies — each is a closed, independent unit.
 * Adding a new policy never touches existing ones.
 */
public final class DiscountPolicies {

    private DiscountPolicies() {}

    /** No discount — null object pattern; avoids null checks at call sites. */
    public static final DiscountPolicy NONE = new DiscountPolicy() {
        @Override public double calculate(double total) { return 0; }
        @Override public String name() { return "NO_DISCOUNT"; }
    };

    /** 10% off for regular loyalty members. */
    public static DiscountPolicy regular() {
        return new DiscountPolicy() {
            @Override public double calculate(double total) { return total * 0.10; }
            @Override public String name() { return "REGULAR_10%"; }
        };
    }

    /** 20% off for VIP members. */
    public static DiscountPolicy vip() {
        return new DiscountPolicy() {
            @Override public double calculate(double total) { return total * 0.20; }
            @Override public String name() { return "VIP_20%"; }
        };
    }

    /**
     * Seasonal flat discount — added without touching any other policy.
     * This is the OCP payoff: new type = new class, zero edits elsewhere.
     */
    public static DiscountPolicy seasonal(double flatAmount) {
        return new DiscountPolicy() {
            @Override public double calculate(double total) {
                return Math.min(flatAmount, total);   // discount cannot exceed total
            }
            @Override public String name() { return "SEASONAL_FLAT_$" + flatAmount; }
        };
    }
}
