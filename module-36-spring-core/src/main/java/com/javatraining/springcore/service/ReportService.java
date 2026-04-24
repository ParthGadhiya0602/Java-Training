package com.javatraining.springcore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Demonstrates setter injection and @Qualifier.
 *
 * <p>Setter injection is useful when the dependency is optional or must be
 * swappable after construction.  @Qualifier("smsNotificationService") selects
 * the SMS bean by name, overriding the @Primary default (EmailNotificationService).
 *
 * <p>In practice, constructor injection is preferred for mandatory dependencies.
 * Setter injection is acceptable for optional or circular-dependency scenarios.
 */
@Service
public class ReportService {

    private NotificationService notificationService;

    @Autowired
    @Qualifier("smsNotificationService")
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public String generateReport(String content) {
        String status = notificationService.notify("Report ready: " + content);
        return "Report[" + content + "] → " + status;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
