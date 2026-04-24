package com.javatraining.microservices.outbox;

import com.javatraining.microservices.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private boolean published;
    private LocalDateTime createdAt;

    public static OutboxEvent forOrder(Order order, String eventType) {
        return OutboxEvent.builder()
                .aggregateType("Order")
                .aggregateId(order.getId())
                .eventType(eventType)
                .payload("{\"orderId\":" + order.getId() + ",\"status\":\"" + order.getStatus() + "\"}")
                .published(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
