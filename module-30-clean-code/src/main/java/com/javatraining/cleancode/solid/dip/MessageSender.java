package com.javatraining.cleancode.solid.dip;

/**
 * DIP - Dependency Inversion Principle.
 *
 * <p><strong>Violation:</strong>
 * <pre>
 *   class AlertService {
 *       private final EmailSender emailSender = new EmailSender();  // ← direct instantiation
 *       // AlertService (high-level) depends on EmailSender (low-level).
 *       // To switch to SMS: modify AlertService - DIP broken.
 *   }
 * </pre>
 *
 * <p><strong>Fix:</strong> both high-level ({@link AlertService}) and low-level
 * ({@link EmailMessageSender}, {@link SmsMessageSender}) depend on this abstraction.
 * AlertService never imports a concrete sender class.
 */
public interface MessageSender {
    String send(String recipient, String message);
}
