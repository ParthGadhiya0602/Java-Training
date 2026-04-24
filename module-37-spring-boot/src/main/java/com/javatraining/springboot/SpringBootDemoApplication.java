package com.javatraining.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @ConfigurationPropertiesScan tells Spring Boot to discover all
 * @ConfigurationProperties classes in this package tree automatically.
 * Without it you'd need @EnableConfigurationProperties(Foo.class) per class.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringBootDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemoApplication.class, args);
    }
}
