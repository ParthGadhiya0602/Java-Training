package com.javatraining.springboot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed config for {@code app.database.*} properties.
 *
 * <p>This is completely independent of Spring Boot's own {@code spring.datasource.*}
 * auto-configuration - it is our custom properties namespace.
 * No database connection is ever made; this class is purely a typed value holder.
 *
 * <p>Profile-specific overrides (e.g. {@code application-dev.properties}) are
 * merged on top of the base {@code application.properties} values at startup.
 */
@ConfigurationProperties(prefix = "app.database")
@Validated
public class DatabaseProperties {

    @NotBlank
    private String url;

    @Min(1)
    private int poolSize;

    @Min(1)
    private int timeoutSeconds;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
