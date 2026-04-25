package com.javatraining.cloud;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Shutdown;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies 12-factor Factor IX - Disposability: graceful shutdown is configured.
 *
 * When Kubernetes sends SIGTERM, Spring Boot:
 *   1. Stops accepting new requests
 *   2. Waits up to spring.lifecycle.timeout-per-shutdown-phase (30s) for in-flight requests
 *   3. Exits cleanly
 *
 * The preStop hook in the K8s Deployment adds a 5-second sleep before SIGTERM is sent,
 * giving the load balancer time to stop routing traffic to the terminating pod.
 */
@SpringBootTest
class GracefulShutdownTest {

    @Autowired ServerProperties serverProperties;

    @Test
    void graceful_shutdown_is_configured() {
        assertThat(serverProperties.getShutdown()).isEqualTo(Shutdown.GRACEFUL);
    }
}
