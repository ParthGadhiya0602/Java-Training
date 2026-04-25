package com.javatraining.microservices.order;

import com.javatraining.microservices.outbox.OutboxEvent;
import com.javatraining.microservices.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Each method runs in its own transaction so the saga can call them independently.
 *
 * createPendingOrder writes the Order and its OutboxEvent in a single transaction -
 * the core guarantee of the outbox pattern: the event is never written without the
 * order, and the order is never written without its event.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Order createPendingOrder(OrderRequest request) {
        Order order = orderRepository.save(Order.builder()
                .productId(request.productId())
                .quantity(request.quantity())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build());
        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCreated"));
        return order;
    }

    @Transactional
    public Order confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CONFIRMED);
        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderConfirmed"));
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELLED);
        outboxEventRepository.save(OutboxEvent.forOrder(order, "OrderCancelled"));
        return orderRepository.save(order);
    }
}
