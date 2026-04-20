package com.javatraining.cleancode.smells;

/**
 * Refactoring: Replace Primitive Obsession with a Value Object.
 *
 * <p><strong>Smell — primitive obsession:</strong>
 * <pre>
 *   // Scattered validation; easy to pass an invalid string
 *   void sendEmail(String email, String subject) { ... }
 *   void register(String name, String email, String phone) { ... }
 * </pre>
 *
 * <p><strong>Fix — dedicated value object:</strong>
 * Validation is centralised in the constructor.  An {@code EmailAddress} instance
 * is always valid by construction — impossible to create an invalid one.
 * Methods that accept EmailAddress are self-documenting.
 */
public record EmailAddress(String value) {

    public EmailAddress {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Email address must not be blank");
        if (!value.contains("@") || value.indexOf('@') == 0 || value.indexOf('@') == value.length() - 1)
            throw new IllegalArgumentException("Email address must contain '@' with text before and after: " + value);
    }

    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override public String toString() { return value; }
}
