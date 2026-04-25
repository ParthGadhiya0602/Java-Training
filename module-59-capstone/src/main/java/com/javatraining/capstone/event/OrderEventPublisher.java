package com.javatraining.capstone.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.capstone.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class OrderEventPublisher {

    static final String TOPIC = "orders";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Order order) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "orderId",    order.getId(),
                    "productId",  order.getProductId(),
                    "quantity",   order.getQuantity(),
                    "status",     order.getStatus().name()
            ));
            kafkaTemplate.send(TOPIC, String.valueOf(order.getId()), payload);
            log.debug("Published order event: orderId={}", order.getId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order event", e);
        }
    }
}
