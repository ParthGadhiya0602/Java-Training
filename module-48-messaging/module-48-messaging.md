# Module 48 — Messaging

## What this module covers

Asynchronous messaging with Apache Kafka and RabbitMQ in a Spring Boot application.
Kafka handles event streaming (ordered, persistent, replayable); RabbitMQ handles
notifications with routing flexibility and dead-letter support.

---

## Project structure

```
src/main/java/com/javatraining/messaging/
├── order/
│   └── OrderEvent.java                   # Kafka payload record
├── kafka/
│   ├── OrderEventProducer.java           # KafkaTemplate publisher
│   ├── OrderEventConsumer.java           # @KafkaListener
│   └── OrderProcessingService.java       # business logic invoked by consumer
└── rabbitmq/
    ├── RabbitConfig.java                 # exchange, queue, DLQ, converter beans
    ├── OrderNotification.java            # RabbitMQ payload record
    ├── OrderNotificationPublisher.java   # RabbitTemplate publisher
    ├── OrderNotificationListener.java    # @RabbitListener
    └── OrderNotificationHandler.java     # business logic invoked by listener
```

---

## Kafka

### Producing

`KafkaTemplate<String, OrderEvent>` sends JSON-serialized records to the topic.

```java
public static final String TOPIC = "order-events";

public void publish(OrderEvent event) {
    kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
}
```

`application.properties` wires the serializers:

```properties
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### Consuming

`@KafkaListener` binds a method to the topic; the consumer group is injected from
properties to avoid hardcoding:

```java
@KafkaListener(topics = OrderEventProducer.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
public void consume(OrderEvent event) {
    processingService.process(event);
}
```

`JsonDeserializer` requires the trusted packages list so it can deserialize
`OrderEvent` without throwing a type-mismatch:

```properties
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.javatraining.messaging.order
```

### Integration testing with EmbeddedKafka

`@EmbeddedKafka` starts an in-process broker before the Spring context and
overrides `spring.kafka.bootstrap-servers` automatically:

```java
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {OrderEventProducer.TOPIC},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class KafkaIntegrationTest {

    @Autowired OrderEventProducer orderEventProducer;
    @SpyBean OrderProcessingService orderProcessingService;
    @MockBean RabbitTemplate rabbitTemplate;   // satisfies RabbitMQ dependency

    @Test
    void consumer_processes_event_published_to_topic() {
        orderEventProducer.publish(new OrderEvent(1L, "CREATED", 10L, 2));

        verify(orderProcessingService, timeout(5000)).process(any(OrderEvent.class));
    }
}
```

`@SpyBean` wraps the real bean so `verify` can assert it was called without
replacing its behaviour. `timeout(5000)` handles the asynchronous gap between
`send` and the listener thread calling `process`.

`@MockBean RabbitTemplate` satisfies `OrderNotificationPublisher`'s dependency
because `RabbitAutoConfiguration` is excluded in the test properties (see below).

#### Test classpath override

`src/test/resources/application.properties` **completely replaces** (not merges
with) `src/main/resources/application.properties` on the test classpath. All
Kafka properties required at startup must be repeated in the test file alongside
the exclusion:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
spring.kafka.consumer.group-id=messaging-module
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.javatraining.messaging.order
```

---

## RabbitMQ

### Exchange, queue, and DLQ

```java
public static final String ORDER_EXCHANGE   = "order.exchange";
public static final String ORDER_QUEUE      = "order.queue";
public static final String ORDER_DLQ        = "order.dlq";
public static final String ORDER_ROUTING_KEY = "order.created";
```

`order.queue` is declared with dead-letter arguments so RabbitMQ routes
undeliverable messages to `order.dlq` automatically:

```java
Queue orderQueue = QueueBuilder.durable(ORDER_QUEUE)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", ORDER_DLQ)
        .build();
```

### Message conversion

A `Jackson2JsonMessageConverter` bean converts POJOs to JSON on publish and back
on consume — no manual serialization needed.

### Producing

```java
public void send(OrderNotification notification) {
    rabbitTemplate.convertAndSend(ORDER_EXCHANGE, ORDER_ROUTING_KEY, notification);
}
```

### Consuming

```java
@RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
public void handleOrderNotification(OrderNotification notification) {
    handler.handle(notification);
}
```

If `handler.handle()` throws, Spring AMQP nacks the message. After retries are
exhausted the dead-letter arguments route it to `order.dlq`.

### Unit tests

Both publisher and listener are tested with plain Mockito — no Spring context:

```java
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
}
```

---

## Tests

| Class                            | Type              | Count |
|----------------------------------|-------------------|-------|
| `KafkaIntegrationTest`           | `@SpringBootTest` | 3     |
| `OrderNotificationPublisherTest` | Mockito unit      | 2     |
| `OrderNotificationListenerTest`  | Mockito unit      | 2     |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **7/7 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `@SpyBean` instead of `@MockBean` for `OrderProcessingService` | Spy wraps the real bean so `verify` works without replacing behaviour |
| `timeout(5000)` in Kafka assertions | Consumer runs on a separate thread; Mockito must wait for the async call |
| `RabbitAutoConfiguration` excluded in test properties | No RabbitMQ broker available during CI; `@MockBean RabbitTemplate` covers the dependency |
| Dead-letter via queue arguments | Keeps routing policy in code alongside the queue declaration |
