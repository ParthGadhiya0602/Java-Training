package com.javatraining.messaging.rabbitmq;

public record OrderNotification(Long orderId, String status) {}
