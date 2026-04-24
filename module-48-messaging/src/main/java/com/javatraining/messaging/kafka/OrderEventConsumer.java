package com.javatraining.messaging.kafka;

import com.javatraining.messaging.order.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @KafkaListener binds this method to the Kafka consumer infrastructure.
 * Spring Kafka polls the broker, deserializes each record, and invokes consume()
 * within a listener container thread.
 *
 * Partition assignment and offset management are handled by the consumer group
 * (spring.kafka.consumer.group-id). With auto-offset-reset=earliest, a new consumer
 * group starts reading from the beginning of the topic.
 *
 * If consume() throws, Spring Kafka applies the configured error handler. By default
 * (SeekToCurrentErrorHandler / DefaultErrorHandler) it retries the record up to 10
 * times before logging and skipping.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderProcessingService processingService;

    @KafkaListener(topics = OrderEventProducer.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OrderEvent event) {
        log.info("Received: orderId={}, status={}", event.orderId(), event.status());
        processingService.process(event);
    }
}
