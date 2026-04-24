package com.javatraining.springcore.config;

/**
 * Plain value object registered as a Spring bean via @Bean in AppConfig.
 * Demonstrates how non-Spring classes become managed beans.
 */
public record AppProperties(String name, String version, int maxConnections) {}
