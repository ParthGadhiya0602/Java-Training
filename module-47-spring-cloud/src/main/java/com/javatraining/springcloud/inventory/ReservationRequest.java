package com.javatraining.springcloud.inventory;

public record ReservationRequest(Long productId, int quantity) {}
