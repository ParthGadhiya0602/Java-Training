package com.javatraining.messaging.kafka;

import com.javatraining.messaging.order.OrderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @EmbeddedKafka starts an in-process Kafka broker before the Spring context.
 * It overrides spring.kafka.bootstrap-servers with the embedded broker's address
 * so the application's KafkaTemplate and @KafkaListener connect to it automatically.
 *
 * RabbitAutoConfiguration is excluded (see src/test/resources/application.properties).
 * @MockBean RabbitTemplate satisfies OrderNotificationPublisher's dependency.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {OrderEventProducer.TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaIntegrationTest {

    @Autowired OrderEventProducer orderEventProducer;
    @SpyBean OrderProcessingService orderProcessingService;
    @MockBean RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        clearInvocations(orderProcessingService);
    }

    @Test
    void consumer_processes_event_published_to_topic() {
        orderEventProducer.publish(new OrderEvent(1L, "CREATED", 10L, 2));

        verify(orderProcessingService, timeout(5000)).process(any(OrderEvent.class));
    }

    @Test
    void consumer_receives_event_with_correct_data() {
        orderEventProducer.publish(new OrderEvent(42L, "CONFIRMED", 99L, 3));

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderProcessingService, timeout(5000)).process(captor.capture());

        assertThat(captor.getValue().orderId()).isEqualTo(42L);
        assertThat(captor.getValue().status()).isEqualTo("CONFIRMED");
    }

    @Test
    void consumer_processes_multiple_events_in_order() {
        orderEventProducer.publish(new OrderEvent(1L, "CREATED", 10L, 1));
        orderEventProducer.publish(new OrderEvent(2L, "CREATED", 11L, 1));
        orderEventProducer.publish(new OrderEvent(3L, "CREATED", 12L, 1));

        verify(orderProcessingService, timeout(10000).times(3)).process(any(OrderEvent.class));
    }
}
