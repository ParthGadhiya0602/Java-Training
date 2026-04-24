package com.javatraining.springcore.service;

import org.springframework.stereotype.Service;

/**
 * Used in AOP tests to trigger @AfterThrowing advice.
 */
@Service
public class OrderService {

    public String placeOrder(String item, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
        return "ORDER[" + item + " x" + quantity + "]";
    }
}
