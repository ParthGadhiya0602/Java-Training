package com.javatraining.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Actuator HTTP endpoints: /health, /info.
 *
 * <p>RANDOM_PORT starts a real embedded Tomcat on a random port so the
 * Actuator HTTP endpoints are accessible via {@link TestRestTemplate}.
 *
 * <p>No active profile — base application.properties + test/application.properties
 * (which exposes all endpoints with management.endpoints.web.exposure.include=*)
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.endpoints.web.exposure.include=*",
                "management.endpoint.health.show-details=always",
                "management.info.env.enabled=true"
        })
class ActuatorTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    // ── /actuator/health ──────────────────────────────────────────────────────

    @Test
    void health_endpoint_returns_200() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void health_endpoint_reports_up() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void custom_health_indicator_contributes_app_component() {
        // AppHealthIndicator contributes a component named "app"
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getBody())
                .contains("\"app\"")
                .contains("Java Training App")
                .contains("maxConnections");
    }

    // ── /actuator/info ────────────────────────────────────────────────────────

    @Test
    void info_endpoint_returns_200() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void info_endpoint_contains_env_info_from_properties() {
        // info.app.* in application.properties → EnvironmentInfoContributor
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info", String.class);
        assertThat(response.getBody())
                .contains("Java Training App")
                .contains("1.0.0");
    }

    @Test
    void info_endpoint_contains_custom_build_contributor() {
        // BuildInfoContributor adds "build" key with artifact + javaVersion
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info", String.class);
        assertThat(response.getBody())
                .contains("\"build\"")
                .contains("spring-boot-demo")
                .contains("javaVersion");
    }
}
