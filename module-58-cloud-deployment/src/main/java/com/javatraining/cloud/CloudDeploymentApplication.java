package com.javatraining.cloud;

import com.javatraining.cloud.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class CloudDeploymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDeploymentApplication.class, args);
    }
}
