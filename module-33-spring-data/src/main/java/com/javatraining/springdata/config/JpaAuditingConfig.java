package com.javatraining.springdata.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing globally.
 *
 * <p>{@code @EnableJpaAuditing} registers the {@code AuditingEntityListener}
 * infrastructure beans that populate {@code @CreatedDate} and
 * {@code @LastModifiedDate} fields before insert/update.
 *
 * <p>In tests that use {@code @DataJpaTest}, this class is NOT loaded by default
 * (it lives outside the slice).  Tests that need auditing supply their own
 * {@code @TestConfiguration} inner class annotated with {@code @EnableJpaAuditing}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
