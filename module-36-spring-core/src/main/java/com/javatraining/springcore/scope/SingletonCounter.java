package com.javatraining.springcore.scope;

import org.springframework.stereotype.Component;

/**
 * Singleton scope (default) — one shared instance per ApplicationContext.
 *
 * <p>Every injection point receives the same object.  State mutations are
 * visible across the whole application.
 */
@Component
public class SingletonCounter {

    private int count = 0;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
