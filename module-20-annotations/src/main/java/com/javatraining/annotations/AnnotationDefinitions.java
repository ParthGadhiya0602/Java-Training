package com.javatraining.annotations;

import java.lang.annotation.*;

/**
 * Module 20 — Defining Custom Annotations
 *
 * An annotation type is declared with @interface.
 * Elements look like methods with optional defaults.
 *
 * Retention policies:
 *   SOURCE  — stripped by javac; visible only in source (e.g. @Override)
 *   CLASS   — stored in .class but not loaded by JVM (default)
 *   RUNTIME — loaded by JVM and readable via reflection
 *
 * Target controls where an annotation may be placed:
 *   TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR,
 *   LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE,
 *   TYPE_PARAMETER, TYPE_USE, MODULE, RECORD_COMPONENT
 *
 * @Documented — annotation appears in Javadoc
 * @Inherited  — subclasses inherit the annotation from their superclass
 * @Repeatable — same annotation may appear more than once on one element
 */
public class AnnotationDefinitions {

    // ── Marker annotation (no elements) ──────────────────────────────────────

    /** Marks a class or method that is not yet ready for production. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Documented
    public @interface Beta {
        String reason() default "work in progress";
    }

    // ── Single-element annotation ─────────────────────────────────────────────

    /**
     * Specifies the author of a class or method.
     * Single-element annotations use the conventional name "value"
     * so callers can omit the key: @Author("Alice").
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Documented
    public @interface Author {
        String value();                // "value" is the conventional single-element name
        String date() default "";
    }

    // ── Multi-element annotation ──────────────────────────────────────────────

    /** Documents a known limitation with an optional severity level. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    public @interface KnownIssue {
        String description();
        Severity severity() default Severity.LOW;

        enum Severity { LOW, MEDIUM, HIGH }
    }

    // ── Repeatable annotation ─────────────────────────────────────────────────

    /**
     * @Repeatable requires a container annotation that holds an array.
     * Usage: @Tag("api") @Tag("public") on the same element.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Tags.class)
    public @interface Tag {
        String value();
    }

    /** Container annotation for @Tag (required for @Repeatable). */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface Tags {
        Tag[] value();
    }

    // ── Array-valued annotation ───────────────────────────────────────────────

    /** Specifies which roles may access a resource. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface RequiresRoles {
        String[] value();                 // array of role names
        boolean allRequired() default false;
    }

    // ── Annotation with Class element ─────────────────────────────────────────

    /** Marks a field as injectable; the implementation class is resolved at runtime. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Inject {
        Class<?> value() default Void.class;  // Void.class as "not specified" sentinel
    }

    // ── Inherited annotation ──────────────────────────────────────────────────

    /**
     * @Inherited: if a class has this annotation, its subclasses automatically
     * inherit it (applies only to class-level annotations, not methods/fields).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface Component {
        String name() default "";
    }

    // ── TYPE_USE annotation ───────────────────────────────────────────────────

    /**
     * TYPE_USE target allows the annotation on any type usage:
     *   @NonNull String name
     *   List<@NonNull String> items
     *   Object obj = (@NonNull Object) raw;
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface NonNull {}
}
