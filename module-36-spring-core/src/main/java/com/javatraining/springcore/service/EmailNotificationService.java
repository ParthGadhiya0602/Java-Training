package com.javatraining.springcore.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @Primary — when multiple beans implement the same interface, Spring
 * injects this one by default unless a @Qualifier overrides the choice.
 */
@Service
@Primary
public class EmailNotificationService implements NotificationService {

    @Override
    public String notify(String message) {
        return "EMAIL: " + message;
    }

    @Override
    public String getType() {
        return "email";
    }
}
