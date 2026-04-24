package com.javatraining.springboot;

import com.javatraining.springboot.config.AppProperties;
import com.javatraining.springboot.config.ConditionalConfig;
import com.javatraining.springboot.config.DatabaseProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies @ConfigurationProperties binding, nested objects, @Validated,
 * and @ConditionalOnProperty / @ConditionalOnMissingBean behaviour.
 *
 * <p>No active profile — base application.properties values apply.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConfigurationPropertiesTest {

    @Autowired AppProperties     appProperties;
    @Autowired DatabaseProperties dbProperties;
    @Autowired ApplicationContext context;

    // ── AppProperties ─────────────────────────────────────────────────────────

    @Test
    void app_name_bound_from_properties() {
        assertThat(appProperties.getName()).isEqualTo("Java Training App");
    }

    @Test
    void max_connections_bound_with_range_validation() {
        assertThat(appProperties.getMaxConnections()).isEqualTo(50);
    }

    @Test
    void nested_feature_flags_notifications_enabled() {
        // app.feature-flags.notifications-enabled=true
        assertThat(appProperties.getFeatureFlags().isNotificationsEnabled()).isTrue();
    }

    @Test
    void nested_feature_flags_analytics_disabled() {
        // app.feature-flags.analytics-enabled=false
        assertThat(appProperties.getFeatureFlags().isAnalyticsEnabled()).isFalse();
    }

    // ── DatabaseProperties ────────────────────────────────────────────────────

    @Test
    void database_url_bound() {
        assertThat(dbProperties.getUrl()).isEqualTo("jdbc:h2:mem:defaultdb");
    }

    @Test
    void database_pool_size_bound() {
        assertThat(dbProperties.getPoolSize()).isEqualTo(10);
    }

    @Test
    void database_timeout_seconds_bound() {
        assertThat(dbProperties.getTimeoutSeconds()).isEqualTo(30);
    }

    // ── @ConditionalOnProperty ────────────────────────────────────────────────

    @Test
    void notification_processor_created_when_notifications_enabled() {
        // notifications-enabled=true → @ConditionalOnProperty satisfied
        assertThat(context.containsBean("notificationProcessor")).isTrue();
        ConditionalConfig.NotificationProcessor processor =
                context.getBean(ConditionalConfig.NotificationProcessor.class);
        assertThat(processor.active()).isTrue();
    }

    @Test
    void analytics_processor_absent_when_analytics_disabled() {
        // analytics-enabled=false → @ConditionalOnProperty not satisfied
        assertThat(context.containsBean("analyticsProcessor")).isFalse();
    }

    // ── @ConditionalOnMissingBean ─────────────────────────────────────────────

    @Test
    void fallback_notification_processor_absent_because_primary_exists() {
        // notificationProcessor IS defined → @ConditionalOnMissingBean skips fallback
        assertThat(context.containsBean("fallbackNotificationProcessor")).isFalse();
        // Only one NotificationProcessor bean exists in the context
        assertThat(context.getBeansOfType(ConditionalConfig.NotificationProcessor.class))
                .hasSize(1);
    }
}
