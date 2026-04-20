package com.javatraining.cleancode.solid.dip;

/** Low-level module — depends on the abstraction, not on AlertService. */
public class EmailMessageSender implements MessageSender {
    @Override
    public String send(String recipient, String message) {
        return "EMAIL → " + recipient + ": " + message;
    }
}
