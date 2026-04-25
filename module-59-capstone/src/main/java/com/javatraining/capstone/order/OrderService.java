package com.javatraining.capstone.order;

import com.javatraining.capstone.event.OrderEventPublisher;
import com.javatraining.capstone.inventory.InventoryClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher eventPublisher;
    private final Counter ordersCreatedCounter;
    private final Timer orderCreationTimer;

    public OrderService(OrderRepository orderRepository,
                        InventoryClient inventoryClient,
                        OrderEventPublisher eventPublisher,
                        MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
        this.ordersCreatedCounter = Counter.builder("orders.created")
                .description("Total orders successfully created")
                .register(meterRegistry);
        this.orderCreationTimer = Timer.builder("orders.creation.duration")
                .description("Time spent creating an order (gRPC check + DB write + Kafka publish)")
                .register(meterRegistry);
    }

    public Order createOrder(String productId, int quantity) {
        return orderCreationTimer.record(() -> {
            boolean available = inventoryClient.checkStock(productId, quantity);
            if (!available) {
                log.warn("Rejected order for {} qty={} — insufficient stock", productId, quantity);
                throw new InsufficientStockException(productId);
            }
            Order order = orderRepository.save(new Order(productId, quantity, OrderStatus.CONFIRMED));
            eventPublisher.publish(order);
            ordersCreatedCounter.increment();
            log.info("Created order id={} product={} qty={}", order.getId(), productId, quantity);
            return order;
        });
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
