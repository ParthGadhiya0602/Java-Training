package com.javatraining.microservices.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table for unpublished events and forwards them to the message broker.
 *
 * The outbox pattern guarantees at-least-once delivery: an event is only removed
 * (marked published) after it has been successfully forwarded. If the process crashes
 * between writing the event and publishing it, the next poll picks it up again.
 *
 * In production, "publish" means sending to Kafka/RabbitMQ. Here it logs the event
 * to keep the demo self-contained.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalse();
        for (OutboxEvent event : pending) {
            log.info("Publishing event: type={}, aggregateId={}, payload={}",
                    event.getEventType(), event.getAggregateId(), event.getPayload());
            event.setPublished(true);
        }
    }
}
