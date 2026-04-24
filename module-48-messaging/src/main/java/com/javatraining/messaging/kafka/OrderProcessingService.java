package com.javatraining.messaging.kafka;

import com.javatraining.messaging.order.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderProcessingService {

    public void process(OrderEvent event) {
        log.info("Processing order: orderId={}, status={}", event.orderId(), event.status());
    }
}
