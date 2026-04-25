package com.javatraining.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demonstrates the conditional bean registration pattern that underpins
 * all of Spring Boot's auto-configuration.
 *
 * <p>Auto-configuration works by shipping @Configuration classes annotated
 * with conditions:
 * <ul>
 *   <li>{@code @ConditionalOnClass} - only if a class is on the classpath
 *       (e.g. configure JPA only when Hibernate is present)</li>
 *   <li>{@code @ConditionalOnMissingBean} - only if the user hasn't already
 *       defined their own bean (lets the user override the default)</li>
 *   <li>{@code @ConditionalOnProperty} - only if a property has the right value
 *       (feature flags, enable/disable switches)</li>
 * </ul>
 */
@Configuration
public class ConditionalConfig {

    /**
     * Created only when {@code app.feature-flags.notifications-enabled=true}.
     * Default application.properties sets this to true → bean IS created.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "app.feature-flags",
            name = "notifications-enabled",
            havingValue = "true")
    public NotificationProcessor notificationProcessor() {
        return new NotificationProcessor(true);
    }

    /**
     * Created only when {@code app.feature-flags.analytics-enabled=true}.
     * Default application.properties sets this to false → bean is NOT created.
     * {@code matchIfMissing = false} means the bean is also absent when the
     * property is not set at all.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "app.feature-flags",
            name = "analytics-enabled",
            havingValue = "true",
            matchIfMissing = false)
    public AnalyticsProcessor analyticsProcessor() {
        return new AnalyticsProcessor(true);
    }

    /**
     * Created only when no other {@link NotificationProcessor} bean exists.
     * Since {@code notificationProcessor()} above IS created (notifications=true),
     * this fallback bean is NOT created.
     *
     * <p>This mirrors how Spring Boot's default auto-configured beans work:
     * "provide a sensible default unless the user supplies their own."
     */
    @Bean
    @ConditionalOnMissingBean(NotificationProcessor.class)
    public NotificationProcessor fallbackNotificationProcessor() {
        return new NotificationProcessor(false);
    }

    // ── Value types ────────────────────────────────────────────────────────────

    public record NotificationProcessor(boolean active) {}
    public record AnalyticsProcessor(boolean active) {}
}
