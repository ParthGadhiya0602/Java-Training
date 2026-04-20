package com.javatraining.springdata.projection;

/**
 * Interface projection — Spring Data creates a JDK proxy that maps
 * getter names to entity field names.  Only the selected columns are fetched.
 *
 * <p>Default methods can compose fields without any extra queries.
 */
public interface EmployeeSummary {

    String getName();

    String getEmail();

    /** Composed field — runs in Java, not SQL; no extra query needed. */
    default String getDisplayName() {
        return getName() + " <" + getEmail() + ">";
    }
}
