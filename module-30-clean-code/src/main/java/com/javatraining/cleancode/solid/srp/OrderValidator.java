package com.javatraining.cleancode.solid.srp;

/**
 * SRP - single responsibility: validate an order.
 * Only changes when validation rules change.
 */
public class OrderValidator {

    public void validate(Order order) {
        if (order == null) throw new IllegalArgumentException("Order must not be null");
        if (order.items().isEmpty()) throw new IllegalArgumentException("Order must have items");
        if (order.total() <= 0) throw new IllegalArgumentException("Order total must be positive");
        if (order.customerEmail() == null || !order.customerEmail().contains("@"))
            throw new IllegalArgumentException("Customer email is invalid");
    }

    public boolean isValid(Order order) {
        try { validate(order); return true; }
        catch (IllegalArgumentException e) { return false; }
    }
}
