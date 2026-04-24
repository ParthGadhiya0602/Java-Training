package com.javatraining.springboot.profile;

/**
 * Abstraction over environment-specific configuration.
 * Different implementations are loaded depending on the active Spring profile.
 */
public interface EnvironmentInfo {
    String getName();
    boolean isDebugEnabled();
    String getDatabaseUrl();
}
