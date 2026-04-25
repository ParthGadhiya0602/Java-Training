package com.javatraining.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies 12-factor Factor III — Config: properties can be overridden externally.
 *
 * @TestPropertySource simulates what happens when environment variables are injected
 * at deploy time (e.g. APP_ENVIRONMENT=staging in Docker / Kubernetes / ECS).
 * Spring Boot maps APP_ENVIRONMENT → app.environment automatically via relaxed binding.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.environment=staging", "app.region=us-east-1"})
class EnvironmentOverrideTest {

    @Autowired MockMvc mockMvc;

    @Test
    void externally_injected_environment_variable_overrides_application_properties() throws Exception {
        mockMvc.perform(get("/api/deployment/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("staging"))
                .andExpect(jsonPath("$.region").value("us-east-1"));
    }
}
