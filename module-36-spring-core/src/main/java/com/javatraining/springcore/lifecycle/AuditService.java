package com.javatraining.springcore.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demonstrates the bean lifecycle callbacks.
 *
 * <p>Lifecycle order:
 * <ol>
 *   <li>Constructor — Spring instantiates the bean</li>
 *   <li>Dependencies injected — @Autowired fields/setters populated</li>
 *   <li>{@code @PostConstruct} — called once, before the bean is used;
 *       safe to reference injected dependencies here</li>
 *   <li>Bean in use — handles application calls</li>
 *   <li>{@code @PreDestroy} — called on context shutdown;
 *       use to close resources (connections, thread pools, files)</li>
 * </ol>
 */
@Service
public class AuditService {

    private final List<String> events = new ArrayList<>();
    private boolean initialized = false;
    private boolean destroyed = false;

    @PostConstruct
    void init() {
        initialized = true;
        events.add("INIT");
    }

    @PreDestroy
    void shutdown() {
        destroyed = true;
        events.add("DESTROY");
    }

    public void record(String event) {
        events.add(event);
    }

    public List<String> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
