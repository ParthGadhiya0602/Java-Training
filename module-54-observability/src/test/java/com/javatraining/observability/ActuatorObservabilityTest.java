package com.javatraining.observability;

import com.javatraining.observability.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorObservabilityTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ProductService productService;

    @Test
    void prometheus_endpoint_returns_200_with_text_plain_content_type() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    @Test
    void prometheus_endpoint_exposes_custom_counter_and_timer_after_service_use() throws Exception {
        // Drive the instrumented service method so its meters appear in the registry
        productService.findById(99L);

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                // Counter: Prometheus appends _total to counter names
                .andExpect(content().string(containsString("products_lookups_total")))
                // Timer: Prometheus appends _seconds to timer names
                .andExpect(content().string(containsString("products_lookup_duration_seconds")));
    }
}
