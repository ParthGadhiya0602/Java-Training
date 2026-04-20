package com.javatraining.cleancode.solid.dip;

import java.util.List;

/**
 * High-level module — depends on the {@link MessageSender} abstraction only.
 * Switching from email to SMS (or adding push notifications) requires zero
 * changes to this class; just inject a different sender at construction time.
 */
public class AlertService {

    private final MessageSender sender;

    public AlertService(MessageSender sender) {
        this.sender = sender;
    }

    public String sendAlert(String recipient, String alertMessage) {
        return sender.send(recipient, "[ALERT] " + alertMessage);
    }

    public List<String> broadcastAlert(List<String> recipients, String alertMessage) {
        return recipients.stream()
                .map(r -> sender.send(r, "[ALERT] " + alertMessage))
                .toList();
    }
}
