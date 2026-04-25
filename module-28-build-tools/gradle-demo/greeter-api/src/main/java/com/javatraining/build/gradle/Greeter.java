package com.javatraining.build.gradle;

/**
 * Public API contract - lives in the greeter-api Gradle subproject.
 * Other subprojects depend on this interface, not on any implementation.
 */
public interface Greeter {
    String greet(String name);
}
