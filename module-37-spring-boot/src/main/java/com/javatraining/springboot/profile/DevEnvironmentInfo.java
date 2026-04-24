package com.javatraining.springboot.profile;

import com.javatraining.springboot.config.DatabaseProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Loaded only when the "dev" profile is active.
 *
 * <p>Profile activation:
 * <ul>
 *   <li>CLI: {@code --spring.profiles.active=dev}</li>
 *   <li>Property: {@code spring.profiles.active=dev} in application.properties</li>
 *   <li>Test: {@code @ActiveProfiles("dev")}</li>
 *   <li>Environment variable: {@code SPRING_PROFILES_ACTIVE=dev}</li>
 * </ul>
 */
@Component
@Profile("dev")
public class DevEnvironmentInfo implements EnvironmentInfo {

    private final DatabaseProperties databaseProperties;

    public DevEnvironmentInfo(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    @Override
    public String getName() { return "development"; }

    @Override
    public boolean isDebugEnabled() { return true; }

    @Override
    public String getDatabaseUrl() { return databaseProperties.getUrl(); }
}
