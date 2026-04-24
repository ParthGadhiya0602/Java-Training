package com.javatraining.microservices.outbox;

import com.javatraining.microservices.order.Order;
import com.javatraining.microservices.order.OrderRepository;
import com.javatraining.microservices.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OutboxPatternTest {

    @Autowired TestEntityManager em;
    @Autowired OrderRepository orderRepository;
    @Autowired OutboxEventRepository outboxEventRepository;

    @Test
    void order_and_outbox_event_are_persisted_atomically() {
        Order order = orderRepository.save(Order.builder()
                .productId(1L).quantity(2).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).build());
        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCreated"));

        em.flush();
        em.clear();

        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);

        OutboxEvent saved = outboxEventRepository.findAll().get(0);
        assertThat(saved.getAggregateId()).isEqualTo(order.getId());
        assertThat(saved.getEventType()).isEqualTo("OrderCreated");
        assertThat(saved.isPublished()).isFalse();
    }

    @Test
    void find_unpublished_returns_only_unpublished_events() {
        Order order = orderRepository.save(Order.builder()
                .productId(1L).quantity(1).status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now()).build());

        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCreated"));

        OutboxEvent alreadyPublished = OutboxEvent.forOrder(order, "OrderConfirmed");
        alreadyPublished.setPublished(true);
        outboxEventRepository.save(alreadyPublished);

        em.flush();
        em.clear();

        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();
        assertThat(unpublished).hasSize(1);
        assertThat(unpublished.get(0).getEventType()).isEqualTo("OrderCreated");
    }

    @Test
    void marking_event_published_removes_it_from_pending_queue() {
        Order order = orderRepository.save(Order.builder()
                .productId(1L).quantity(1).status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).build());
        OutboxEvent event = outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCreated"));
        em.flush();
        em.clear();

        OutboxEvent loaded = outboxEventRepository.findById(event.getId()).orElseThrow();
        loaded.setPublished(true);
        outboxEventRepository.save(loaded);
        em.flush();
        em.clear();

        assertThat(outboxEventRepository.findByPublishedFalse()).isEmpty();
    }
}
