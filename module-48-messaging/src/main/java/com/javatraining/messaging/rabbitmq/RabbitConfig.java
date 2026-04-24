package com.javatraining.messaging.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dead-letter queue (DLQ) setup:
 *
 *   order.queue  ──(reject/expire)──►  order.exchange  ──(order.dead)──►  order.dlq
 *
 * When a consumer throws an exception and Spring AMQP exhausts its retries,
 * it nacks the message with requeue=false. RabbitMQ then applies the
 * x-dead-letter-* arguments on order.queue and routes the rejected message
 * to order.dlq via order.exchange with routing key "order.dead".
 *
 * This pattern allows failed messages to be inspected and replayed
 * without blocking the main queue.
 */
@Configuration
public class RabbitConfig {

    public static final String ORDER_EXCHANGE    = "order.exchange";
    public static final String ORDER_QUEUE       = "order.queue";
    public static final String ORDER_ROUTING_KEY = "order.created";
    public static final String ORDER_DLQ         = "order.dlq";
    public static final String ORDER_DLQ_KEY     = "order.dead";

    @Bean
    DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(ORDER_DLQ).build();
    }

    @Bean
    Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    @Bean
    Binding dlqBinding(Queue deadLetterQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(orderExchange).with(ORDER_DLQ_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
