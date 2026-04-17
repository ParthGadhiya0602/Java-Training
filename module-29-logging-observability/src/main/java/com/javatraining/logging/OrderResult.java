package com.javatraining.logging;

public record OrderResult(String orderId, OrderStatus status, String message) {}
