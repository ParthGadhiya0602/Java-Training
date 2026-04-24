package com.javatraining.messaging.kafka;

import com.javatraining.messaging.order.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    static final String TOPIC = "order-events";

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
        log.info("Published: orderId={}, status={}", event.orderId(), event.status());
    }
}
