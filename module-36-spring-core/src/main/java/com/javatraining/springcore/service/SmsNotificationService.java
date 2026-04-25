package com.javatraining.springcore.service;

import org.springframework.stereotype.Service;

/**
 * Secondary implementation - injected by name via {@code @Qualifier("smsNotificationService")}.
 */
@Service
public class SmsNotificationService implements NotificationService {

    @Override
    public String notify(String message) {
        return "SMS: " + message;
    }

    @Override
    public String getType() {
        return "sms";
    }
}
