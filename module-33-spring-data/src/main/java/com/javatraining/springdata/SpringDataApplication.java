package com.javatraining.springdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal entry point required by {@code @DataJpaTest} to resolve the base
 * package for entity and repository scanning.
 *
 * <p>This module focuses on the persistence layer; no web layer is present.
 */
@SpringBootApplication
public class SpringDataApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDataApplication.class, args);
    }
}
