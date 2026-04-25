package com.javatraining.cleancode.solid.dip;

/** Low-level module - swappable without touching AlertService. */
public class SmsMessageSender implements MessageSender {
    @Override
    public String send(String recipient, String message) {
        return "SMS → " + recipient + ": " + message;
    }
}
