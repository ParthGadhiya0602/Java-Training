package com.javatraining.messaging.order;

public record OrderEvent(Long orderId, String status, Long productId, int quantity) {}
