package com.javatraining.messaging.rabbitmq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotificationPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @InjectMocks OrderNotificationPublisher publisher;

    @Test
    void sends_to_correct_exchange_and_routing_key() {
        publisher.send(new OrderNotification(1L, "CREATED"));

        verify(rabbitTemplate).convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_ROUTING_KEY,
                new OrderNotification(1L, "CREATED")
        );
    }

    @Test
    void sends_notification_with_correct_payload() {
        publisher.send(new OrderNotification(99L, "CONFIRMED"));

        ArgumentCaptor<OrderNotification> captor = ArgumentCaptor.forClass(OrderNotification.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), captor.capture());

        assertThat(captor.getValue().orderId()).isEqualTo(99L);
        assertThat(captor.getValue().status()).isEqualTo("CONFIRMED");
    }
}
