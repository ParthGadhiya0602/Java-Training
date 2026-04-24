package com.javatraining.springcore.scope;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Prototype scope — a new instance is created every time the bean is requested
 * from the context (via ApplicationContext.getBean() or ObjectProvider.getObject()).
 *
 * <p>Key difference from singleton:
 * <ul>
 *   <li>Singleton: same instance, shared state</li>
 *   <li>Prototype: fresh instance per request, isolated state</li>
 * </ul>
 *
 * <p>Spring does NOT call @PreDestroy on prototype beans — the caller is
 * responsible for lifecycle management.
 */
@Component
@Scope("prototype")
public class PrototypeTask {

    private static int instanceCount = 0;

    private final int instanceId;
    private String status = "NEW";

    public PrototypeTask() {
        instanceId = ++instanceCount;
    }

    public void execute() {
        status = "DONE";
    }

    public int getInstanceId() {
        return instanceId;
    }

    public String getStatus() {
        return status;
    }

    public static void resetCounter() {
        instanceCount = 0;
    }
}
