package com.javatraining.microservices.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.microservices.saga.OrderCreationSaga;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean OrderCreationSaga orderCreationSaga;

    @Test
    void create_order_returns_201_with_confirmed_order() throws Exception {
        Order confirmed = Order.builder().id(1L).productId(1L).quantity(2)
                .status(OrderStatus.CONFIRMED).build();
        when(orderCreationSaga.execute(any())).thenReturn(confirmed);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void create_order_with_zero_quantity_returns_400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_order_with_null_product_id_returns_400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(null, 2))))
                .andExpect(status().isBadRequest());
    }
}
