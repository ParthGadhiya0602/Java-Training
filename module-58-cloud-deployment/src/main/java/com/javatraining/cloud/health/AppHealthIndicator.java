package com.javatraining.cloud.health;

import com.javatraining.cloud.config.AppProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator surfaced at /actuator/health.
 *
 * Kubernetes liveness and readiness probes hit /actuator/health/liveness and
 * /actuator/health/readiness respectively (enabled in application.properties).
 * This indicator contributes to the aggregate health status.
 */
@Component
public class AppHealthIndicator implements HealthIndicator {

    private final AppProperties appProperties;

    public AppHealthIndicator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("environment", appProperties.environment())
                .withDetail("version", appProperties.version())
                .withDetail("region", appProperties.region() != null ? appProperties.region() : "unknown")
                .build();
    }
}
