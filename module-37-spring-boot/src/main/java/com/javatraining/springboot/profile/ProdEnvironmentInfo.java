package com.javatraining.springboot.profile;

import com.javatraining.springboot.config.DatabaseProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Loaded only when the "prod" profile is active.
 * Production profile overrides database URL, pool size, and disables debug.
 */
@Component
@Profile("prod")
public class ProdEnvironmentInfo implements EnvironmentInfo {

    private final DatabaseProperties databaseProperties;

    public ProdEnvironmentInfo(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    @Override
    public String getName() { return "production"; }

    @Override
    public boolean isDebugEnabled() { return false; }

    @Override
    public String getDatabaseUrl() { return databaseProperties.getUrl(); }
}
