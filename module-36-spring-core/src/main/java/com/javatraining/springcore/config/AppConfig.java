package com.javatraining.springcore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demonstrates the @Configuration + @Bean style of bean registration.
 *
 * <p>Use @Configuration when:
 * <ul>
 *   <li>The class lives in a third-party library (can't add @Component)</li>
 *   <li>You need to configure the bean with parameters before registration</li>
 *   <li>You want to make bean wiring explicit and centralized</li>
 * </ul>
 *
 * <p>{@code @Configuration} classes use CGLIB proxying so that @Bean methods
 * called from within the same config class return the same singleton instance.
 */
@Configuration
public class AppConfig {

    @Bean
    public AppProperties appProperties() {
        return new AppProperties("Java Training App", "1.0.0", 100);
    }
}
