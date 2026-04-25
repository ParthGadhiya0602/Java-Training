package com.javatraining.springcore.service;

/**
 * Simple interface used to demonstrate:
 * 1. Multiple implementations - @Primary / @Qualifier selection
 * 2. Setter injection example in {@link ReportService}
 */
public interface NotificationService {
    String notify(String message);
    String getType();
}
