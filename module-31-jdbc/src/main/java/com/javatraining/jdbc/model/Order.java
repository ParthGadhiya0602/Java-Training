package com.javatraining.jdbc.model;

import java.math.BigDecimal;

/**
 * Immutable order value object.
 *
 * <p>An order is created atomically by
 * {@link com.javatraining.jdbc.repository.OrderRepository#placeOrder}:
 * stock is deducted and the record inserted within a single transaction.
 */
public record Order(int id, int productId, int quantity, BigDecimal totalPrice, String status) {}
