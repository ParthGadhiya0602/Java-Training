package com.javatraining.springboot.actuator;

import com.javatraining.springboot.config.AppProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom {@link HealthIndicator} — contributes an "app" component to the
 * /actuator/health response.
 *
 * <p>Spring Boot's Actuator auto-discovers all {@link HealthIndicator} beans
 * and aggregates them under the "components" key of the health response.
 * The overall status is the worst of all individual component statuses.
 *
 * <p>Built-in indicators: diskSpace, ping (always UP), db (if DataSource present),
 * redis (if RedisTemplate present), etc.
 */
@Component
public class AppHealthIndicator implements HealthIndicator {

    private final AppProperties appProperties;

    public AppHealthIndicator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Health health() {
        if (appProperties.getMaxConnections() > 0) {
            return Health.up()
                    .withDetail("app", appProperties.getName())
                    .withDetail("maxConnections", appProperties.getMaxConnections())
                    .build();
        }
        return Health.down()
                .withDetail("reason", "maxConnections must be positive")
                .build();
    }
}
