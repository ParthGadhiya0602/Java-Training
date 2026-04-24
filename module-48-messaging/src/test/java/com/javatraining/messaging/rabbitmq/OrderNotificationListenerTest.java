package com.javatraining.messaging.rabbitmq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderNotificationListenerTest {

    @Mock OrderNotificationHandler handler;
    @InjectMocks OrderNotificationListener listener;

    @Test
    void delegates_notification_to_handler() {
        OrderNotification notification = new OrderNotification(1L, "CREATED");

        listener.handleOrderNotification(notification);

        verify(handler).handle(notification);
    }

    @Test
    void exception_from_handler_propagates_to_trigger_dlq_routing() {
        doThrow(new RuntimeException("processing failed")).when(handler).handle(any());

        assertThatThrownBy(() -> listener.handleOrderNotification(new OrderNotification(1L, "CREATED")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("processing failed");
    }
}
