package com.javatraining.lombokstruct;

import lombok.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the four most important Lombok pitfalls.
 * No Spring context needed — these are pure Java behaviour demonstrations.
 */
class LombokPitfallsTest {

    // ── Pitfall 1: Mutable fields in @EqualsAndHashCode break collections ─────

    /**
     * @Data uses @EqualsAndHashCode on ALL fields by default.
     * If any of those fields change after the object is added to a HashSet
     * or used as a HashMap key, the bucket hash changes and the object is
     * effectively "lost" in the collection.
     */
    @Data
    static class MutableKey {
        private String id;
        private String email;  // mutable — included in hashCode
    }

    @Test
    void mutable_equals_hashcode_breaks_hashset_lookup() {
        MutableKey key = new MutableKey();
        key.setId("u1");
        key.setEmail("before@example.com");

        Set<MutableKey> set = new HashSet<>();
        set.add(key);
        assertThat(set).contains(key);  // OK: hash matches bucket

        key.setEmail("after@example.com");  // mutate! hashCode changes

        // HashSet.contains() computes the NEW hash → looks in the WRONG bucket → false.
        // Must call set.contains() directly: AssertJ's doesNotContain() uses equals()
        // on each element which would compare the same reference and return true,
        // masking the real broken-bucket problem.
        assertThat(set.contains(key)).isFalse();
        // The set still has 1 element — the object is present but unreachable
        assertThat(set).hasSize(1);
    }

    // ── Pitfall 2: @Builder.Default required for collection field initializers ─

    /**
     * Without @Builder.Default, field initializers are ignored by the builder.
     * The builder sets every unspecified field to its Java default (null for objects).
     */
    @Getter
    @Builder
    static class TaskWithoutDefault {
        @Builder.Default
        private List<String> tags = new ArrayList<>();  // @Builder.Default preserves initializer

        private String title;
    }

    @Getter
    @Builder
    static class TaskBroken {
        // Missing @Builder.Default: builder ignores this initializer → null
        private List<String> brokenTags = new ArrayList<>();
        private String title;
    }

    @Test
    void builder_default_preserves_collection_initializer() {
        TaskWithoutDefault task = TaskWithoutDefault.builder().title("My Task").build();
        // @Builder.Default: initializer runs → empty list, not null
        assertThat(task.getTags()).isNotNull().isEmpty();
    }

    @Test
    void builder_without_default_produces_null_collection() {
        TaskBroken task = TaskBroken.builder().title("Broken Task").build();
        // No @Builder.Default: builder uses Java's default value (null) instead of new ArrayList<>()
        assertThat(task.getBrokenTags()).isNull();
    }

    // ── Pitfall 3: @Builder removes the implicit no-args constructor ──────────

    /**
     * When @Builder is the only annotation, Lombok generates an all-args constructor
     * (package-private) for the builder to use. This removes Java's implicit
     * no-args constructor — breaking Jackson deserialization, JPA proxy creation,
     * and any code using new Foo().
     *
     * <p>Fix: explicitly add @NoArgsConstructor + @AllArgsConstructor alongside @Builder.
     */
    @Getter
    @Builder
    @NoArgsConstructor   // FIX: explicit no-args constructor
    @AllArgsConstructor  // FIX: required so @Builder has an all-args constructor to call
    static class FixedUserBean {
        private String name;
        private String email;
    }

    @Test
    void builder_and_no_args_constructor_coexist_when_declared_explicitly() {
        // no-args constructor works (needed by Jackson, JPA, etc.)
        FixedUserBean fromNoArgs = new FixedUserBean();
        // Fields default to null — no-args constructor doesn't throw
        assertThat(fromNoArgs.getName()).isNull();

        // Builder also works
        FixedUserBean fromBuilder = FixedUserBean.builder()
                .name("Bob")
                .email("bob@example.com")
                .build();

        assertThat(fromBuilder.getName()).isEqualTo("Bob");
        assertThat(fromBuilder.getEmail()).isEqualTo("bob@example.com");
    }

    // ── Pitfall 4: @ToString circular reference causes StackOverflowError ─────

    /**
     * If two classes reference each other and both have @ToString without exclusions,
     * calling toString() on either triggers infinite recursion.
     *
     * <p>Fix: exclude the back-reference using @ToString(exclude = "field") or
     * @ToString.Exclude on the field.
     */
    @Getter @Setter
    @ToString(exclude = "manager")   // FIX: exclude back-reference to break the cycle
    static class Employee {
        private String name;
        private Manager manager;
    }

    @Getter @Setter
    @ToString
    static class Manager {
        private String name;
        // Holds a list of reports — not circular because Employee excludes manager
        private List<Employee> reports = new ArrayList<>();
    }

    @Test
    void toString_exclude_prevents_circular_stackoverflow() {
        Manager manager = new Manager();
        manager.setName("Carol");

        Employee emp = new Employee();
        emp.setName("Dave");
        emp.setManager(manager);
        manager.getReports().add(emp);

        // Would throw StackOverflowError without @ToString(exclude = "manager")
        String employeeStr = emp.toString();
        assertThat(employeeStr).contains("Dave").doesNotContain("Carol");

        String managerStr = manager.toString();
        assertThat(managerStr).contains("Carol").contains("Dave");
    }
}
