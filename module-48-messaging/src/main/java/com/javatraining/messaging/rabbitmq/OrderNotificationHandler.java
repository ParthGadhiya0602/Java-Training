package com.javatraining.messaging.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderNotificationHandler {

    public void handle(OrderNotification notification) {
        log.info("Handling notification: orderId={}, status={}", notification.orderId(), notification.status());
    }
}
