package com.javatraining.capstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.capstone.inventory.InventoryClient;
import com.javatraining.capstone.notification.NotificationListener;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test: REST → OrderService → Kafka → NotificationListener.
 *
 * InventoryClient is @MockBean so the gRPC path is tested separately in
 * InventoryGrpcServiceTest. Embedded Kafka exercises the real producer/consumer path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(
        partitions = 1,
        topics = {"orders"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class OrderFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired NotificationListener notificationListener;
    @Autowired MeterRegistry meterRegistry;
    @Autowired ObjectMapper objectMapper;

    @MockBean InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        when(inventoryClient.checkStock(anyString(), anyInt())).thenReturn(true);
    }

    @Test
    @WithMockUser(roles = "USER")
    void order_creation_returns_201_and_event_is_consumed_by_notification_listener() throws Exception {
        int countBefore = notificationListener.getProcessedCount();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"PROD-1","quantity":2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value("PROD-1"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Wait for the Kafka consumer to process the event
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> notificationListener.getProcessedCount() > countBefore);
    }

    @Test
    @WithMockUser(roles = "USER")
    void created_order_can_be_retrieved_by_id() throws Exception {
        String body = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"PROD-2","quantity":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.productId").value("PROD-2"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void order_creation_increments_orders_created_metric() throws Exception {
        double before = meterRegistry.counter("orders.created").count();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"PROD-3","quantity":5}
                                """))
                .andExpect(status().isCreated());

        assertThat(meterRegistry.counter("orders.created").count()).isEqualTo(before + 1);
    }
}
