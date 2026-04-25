package com.javatraining.cleancode.solid.ocp;

/**
 * OCP - high-level engine that is closed for modification.
 * It depends on the {@link DiscountPolicy} abstraction, not on concrete types.
 * Adding a new discount type never requires editing this class.
 */
public class PricingEngine {

    private final DiscountPolicy policy;

    public PricingEngine(DiscountPolicy policy) {
        this.policy = policy;
    }

    public double finalPrice(double orderTotal) {
        double discount = policy.calculate(orderTotal);
        return Math.max(0, orderTotal - discount);
    }

    public String appliedPolicy() {
        return policy.name();
    }
}
