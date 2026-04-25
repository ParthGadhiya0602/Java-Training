package com.javatraining.springboot.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Custom {@link InfoContributor} - adds a "build" section to /actuator/info.
 *
 * <p>Spring Boot auto-discovers all {@link InfoContributor} beans and merges
 * their contributions into the /actuator/info response.
 *
 * <p>Built-in contributors (when enabled):
 * <ul>
 *   <li>EnvironmentInfoContributor - reads {@code info.*} properties
 *       (requires {@code management.info.env.enabled=true})</li>
 *   <li>GitInfoContributor - reads git.properties (from git-commit-id plugin)</li>
 *   <li>BuildInfoContributor - reads META-INF/build-info.properties
 *       (from spring-boot-maven-plugin with buildInfo goal)</li>
 * </ul>
 */
@Component
public class BuildInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("build", Map.of(
                "artifact", "spring-boot-demo",
                "javaVersion", System.getProperty("java.version"),
                "springBootVersion", org.springframework.boot.SpringBootVersion.getVersion()
        ));
    }
}
