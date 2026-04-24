package com.javatraining.lombokstruct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Demonstrates two of the most impactful Lombok annotations for services.
 *
 * <p>@Slf4j generates:
 * <pre>
 *     private static final org.slf4j.Logger log =
 *         org.slf4j.LoggerFactory.getLogger(NotificationService.class);
 * </pre>
 * No boilerplate, no copy-paste of the class name, no import management.
 * The generated field is named {@code log}.
 *
 * <p>@RequiredArgsConstructor generates a constructor for every {@code final}
 * or {@code @NonNull} field. Combined with Spring, this means no {@code @Autowired}
 * annotation is needed — Spring uses the single constructor automatically.
 * Non-final fields (mutable state) are excluded from the constructor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailGateway emailGateway;

    // Non-final — NOT included in the @RequiredArgsConstructor constructor
    private int sentCount = 0;

    public String send(String recipient, String message) {
        log.info("Sending message to {}: {}", recipient, message);
        sentCount++;
        return emailGateway.deliver(recipient, message);
    }

    public void sendBatch(List<String> recipients, String message) {
        log.debug("Starting batch send to {} recipients", recipients.size());
        recipients.forEach(r -> send(r, message));
        log.info("Batch complete — sent {} messages", recipients.size());
    }

    public int getSentCount() {
        return sentCount;
    }
}
