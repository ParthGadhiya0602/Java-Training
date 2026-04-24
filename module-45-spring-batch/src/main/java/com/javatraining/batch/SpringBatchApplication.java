package com.javatraining.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3.x batch application.
 *
 * Do NOT add @EnableBatchProcessing — it conflicts with Spring Boot's batch
 * auto-configuration in 3.x. Boot auto-configures JobRepository, JobLauncher,
 * JobExplorer, and the metadata schema automatically.
 */
@SpringBootApplication
public class SpringBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBatchApplication.class, args);
    }
}
