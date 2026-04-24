package com.javatraining.messaging.rabbitmq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @RabbitListener binds this method to order.queue.
 *
 * Message conversion: Spring AMQP uses the Jackson2JsonMessageConverter bean
 * (from RabbitConfig) to deserialise the JSON payload into OrderNotification.
 *
 * If handle() throws any exception, Spring AMQP's default error handler
 * (SimpleRabbitListenerContainerFactory) catches it and, after retries are
 * exhausted, nacks the message with requeue=false. RabbitMQ then applies the
 * dead-letter arguments on order.queue and routes the message to order.dlq.
 *
 * To reject immediately without retrying, throw AmqpRejectAndDontRequeueException.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

    private final OrderNotificationHandler handler;

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    public void handleOrderNotification(OrderNotification notification) {
        handler.handle(notification);
    }
}
