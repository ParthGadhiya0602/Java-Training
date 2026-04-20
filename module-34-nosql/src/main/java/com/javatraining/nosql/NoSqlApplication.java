package com.javatraining.nosql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point — required for {@code @DataMongoTest} to resolve the base package
 * for document and repository scanning.
 */
@SpringBootApplication
public class NoSqlApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoSqlApplication.class, args);
    }
}
