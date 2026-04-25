package com.javatraining.cleancode.solid.srp;

/**
 * SRP - single responsibility: compose and send notifications.
 * Only changes when the notification format or channel changes.
 */
public class NotificationService {

    public String buildConfirmationMessage(Order order) {
        return "Dear " + order.customerName()
               + ", your order #" + order.id() + " is confirmed."
               + " Total: $" + order.total();
    }
}
