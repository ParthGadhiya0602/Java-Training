package com.javatraining.cloud;

import com.javatraining.cloud.health.AppHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the custom health indicator and the /actuator/health endpoint.
 *
 * Cloud load balancers (AWS ALB, GCP Cloud Load Balancing) and Kubernetes probes
 * poll /actuator/health to decide whether to route traffic to a pod.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthIndicatorTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppHealthIndicator appHealthIndicator;

    @Test
    void custom_health_indicator_reports_up_with_deployment_details() {
        var health = appHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("environment");
        assertThat(health.getDetails()).containsKey("version");
    }

    @Test
    void actuator_health_endpoint_returns_200_with_up_status() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
