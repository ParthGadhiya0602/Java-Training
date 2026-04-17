package com.javatraining.build.gradle;

/**
 * English implementation of {@link Greeter}.
 * Lives in greeter-impl, which depends on greeter-api via
 * {@code implementation(project(":greeter-api"))} in build.gradle.kts.
 */
public class EnglishGreeter implements Greeter {

    @Override
    public String greet(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name must not be blank");
        return "Hello, " + name.strip() + "!";
    }
}
