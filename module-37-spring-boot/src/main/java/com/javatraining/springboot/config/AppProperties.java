package com.javatraining.springboot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed, validated configuration bound from {@code app.*} properties.
 *
 * <p>Spring Boot's relaxed binding rules:
 * <ul>
 *   <li>{@code app.max-connections} → {@code maxConnections}</li>
 *   <li>{@code app.feature-flags.notifications-enabled} → {@code featureFlags.notificationsEnabled}</li>
 * </ul>
 *
 * <p>{@code @Validated} triggers Bean Validation on startup — a misconfigured
 * property fails fast with a clear error rather than a mysterious NullPointerException
 * somewhere deep in the application.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    @NotBlank
    private String name;

    @Min(1) @Max(200)
    private int maxConnections;

    @Valid
    private FeatureFlags featureFlags = new FeatureFlags();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

    public FeatureFlags getFeatureFlags() { return featureFlags; }
    public void setFeatureFlags(FeatureFlags featureFlags) { this.featureFlags = featureFlags; }

    /**
     * Nested properties object — bound from {@code app.feature-flags.*}.
     * Must be a mutable class (not a record) for relaxed binding to work.
     */
    public static class FeatureFlags {
        private boolean notificationsEnabled;
        private boolean analyticsEnabled;

        public boolean isNotificationsEnabled() { return notificationsEnabled; }
        public void setNotificationsEnabled(boolean v) { this.notificationsEnabled = v; }

        public boolean isAnalyticsEnabled() { return analyticsEnabled; }
        public void setAnalyticsEnabled(boolean v) { this.analyticsEnabled = v; }
    }
}
