package com.javatraining.lombokstruct.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub email gateway — injected into NotificationService to demonstrate
 * @RequiredArgsConstructor wiring.
 */
@Component
@Slf4j
public class EmailGateway {

    public String deliver(String recipient, String message) {
        log.debug("Delivering to {}", recipient);
        return "DELIVERED[" + recipient + "]: " + message;
    }
}
