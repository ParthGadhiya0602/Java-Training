package com.javatraining.cleancode.solid.isp;

/**
 * ISP - Interface Segregation Principle.
 *
 * <p><strong>Violation (fat interface):</strong>
 * <pre>
 *   interface Worker {
 *       void work();
 *       void eat();    // Robots don't eat
 *       void charge(); // Humans don't charge
 *   }
 *   class Robot implements Worker {
 *       public void eat() { throw new UnsupportedOperationException(); }  // forced stub
 *   }
 * </pre>
 *
 * <p><strong>Fix:</strong> split into focused interfaces.
 * Clients depend only on the methods they actually use.
 */
public interface Workable {
    String work();
}
