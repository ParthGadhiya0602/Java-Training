package com.javatraining.cloud.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 12-factor app Factor III — Config: all configuration loaded from environment.
 *
 * In production these values come from environment variables (Spring Boot maps
 * APP_ENVIRONMENT → app.environment, APP_REGION → app.region, etc.) or from a
 * secrets manager (Vault, AWS Secrets Manager, GCP Secret Manager).
 *
 * The record constructor binding enforces required properties at startup via
 * @NotBlank — the app refuses to start if a required value is missing.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        @NotBlank String name,
        @NotBlank String version,
        @NotBlank String environment,
        String region
) {}
