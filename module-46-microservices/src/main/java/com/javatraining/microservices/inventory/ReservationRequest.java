package com.javatraining.microservices.inventory;

public record ReservationRequest(Long productId, int quantity) {}
