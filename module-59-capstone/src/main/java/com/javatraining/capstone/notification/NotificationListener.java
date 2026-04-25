package com.javatraining.capstone.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class NotificationListener {

    private final AtomicInteger processedCount = new AtomicInteger(0);

    @KafkaListener(topics = "orders", groupId = "notifications")
    public void onOrderEvent(String message) {
        log.info("Notification received — order event: {}", message);
        processedCount.incrementAndGet();
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}
