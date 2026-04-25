package com.javatraining.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the deployment info endpoint and actuator info.
 *
 * 12-factor Factor III (Config): all values come from application.properties
 * which can be overridden by environment variables at runtime.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DeploymentInfoTest {

    @Autowired MockMvc mockMvc;

    @Test
    void deployment_info_endpoint_returns_all_app_metadata() throws Exception {
        mockMvc.perform(get("/api/deployment/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("cloud-deployment-demo"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.environment").value("local"))
                .andExpect(jsonPath("$.region").value("local"));
    }

    @Test
    void actuator_info_endpoint_exposes_app_metadata() throws Exception {
        // /actuator/info surfaces info.* properties — driven by app.* via property references
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("cloud-deployment-demo"))
                .andExpect(jsonPath("$.app.version").value("1.0.0"))
                .andExpect(jsonPath("$.app.environment").value("local"));
    }
}
