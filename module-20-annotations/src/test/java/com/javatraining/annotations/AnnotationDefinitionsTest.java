package com.javatraining.annotations;

import com.javatraining.annotations.AnnotationDefinitions.Author;
import com.javatraining.annotations.AnnotationDefinitions.Beta;
import com.javatraining.annotations.AnnotationDefinitions.Component;
import com.javatraining.annotations.AnnotationDefinitions.KnownIssue;
import com.javatraining.annotations.AnnotationDefinitions.RequiresRoles;
import com.javatraining.annotations.AnnotationDefinitions.Tag;
import com.javatraining.annotations.AnnotationDefinitions.Tags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnnotationDefinitions")
class AnnotationDefinitionsTest {

    @Nested
    @DisplayName("@Beta")
    class BetaTests {
        @Test void beta_has_runtime_retention() {
            Retention r = Beta.class.getAnnotation(Retention.class);
            assertNotNull(r);
            assertEquals(RetentionPolicy.RUNTIME, r.value());
        }

        @Test void beta_default_reason() throws Exception {
            @Beta
            class Marked {}
            Beta b = Marked.class.getAnnotation(Beta.class);
            assertNotNull(b);
            assertEquals("work in progress", b.reason());
        }

        @Test void beta_custom_reason() throws Exception {
            @Beta(reason = "experimental")
            class Marked {}
            Beta b = Marked.class.getAnnotation(Beta.class);
            assertEquals("experimental", b.reason());
        }
    }

    @Nested
    @DisplayName("@Author")
    class AuthorTests {
        @Test void author_value_element() throws Exception {
            @Author("Alice")
            class Marked {}
            Author a = Marked.class.getAnnotation(Author.class);
            assertNotNull(a);
            assertEquals("Alice", a.value());
        }

        @Test void author_date_defaults_to_empty() throws Exception {
            @Author("Bob")
            class Marked {}
            Author a = Marked.class.getAnnotation(Author.class);
            assertEquals("", a.date());
        }

        @Test void author_with_date() throws Exception {
            @Author(value = "Carol", date = "2024-06-01")
            class Marked {}
            Author a = Marked.class.getAnnotation(Author.class);
            assertEquals("Carol", a.value());
            assertEquals("2024-06-01", a.date());
        }
    }

    @Nested
    @DisplayName("@KnownIssue")
    class KnownIssueTests {
        @Test void known_issue_description_and_severity() throws Exception {
            @KnownIssue(description = "slow query", severity = KnownIssue.Severity.HIGH)
            class Marked {}
            KnownIssue ki = Marked.class.getAnnotation(KnownIssue.class);
            assertNotNull(ki);
            assertEquals("slow query", ki.description());
            assertEquals(KnownIssue.Severity.HIGH, ki.severity());
        }

        @Test void known_issue_default_severity_is_low() throws Exception {
            @KnownIssue(description = "minor thing")
            class Marked {}
            KnownIssue ki = Marked.class.getAnnotation(KnownIssue.class);
            assertEquals(KnownIssue.Severity.LOW, ki.severity());
        }
    }

    @Nested
    @DisplayName("@Tag (repeatable)")
    class TagTests {
        @Test void single_tag_readable() throws Exception {
            @Tag("api")
            class Marked {}
            Tag[] tags = Marked.class.getAnnotationsByType(Tag.class);
            assertEquals(1, tags.length);
            assertEquals("api", tags[0].value());
        }

        @Test void multiple_tags_readable() throws Exception {
            @Tag("api")
            @Tag("public")
            class Marked {}
            Tag[] tags = Marked.class.getAnnotationsByType(Tag.class);
            assertEquals(2, tags.length);
        }

        @Test void tags_container_holds_array() throws Exception {
            @Tag("x")
            @Tag("y")
            @Tag("z")
            class Marked {}
            Tags container = Marked.class.getAnnotation(Tags.class);
            assertNotNull(container);
            assertEquals(3, container.value().length);
        }
    }

    @Nested
    @DisplayName("@RequiresRoles")
    class RequiresRolesTests {
        @Test void roles_array_readable() throws Exception {
            @RequiresRoles({"admin", "user"})
            class Marked {}
            RequiresRoles rr = Marked.class.getAnnotation(RequiresRoles.class);
            assertNotNull(rr);
            assertArrayEquals(new String[]{"admin", "user"}, rr.value());
        }

        @Test void all_required_defaults_to_false() throws Exception {
            @RequiresRoles({"admin"})
            class Marked {}
            RequiresRoles rr = Marked.class.getAnnotation(RequiresRoles.class);
            assertFalse(rr.allRequired());
        }

        @Test void all_required_can_be_set_true() throws Exception {
            @RequiresRoles(value = {"superuser"}, allRequired = true)
            class Marked {}
            RequiresRoles rr = Marked.class.getAnnotation(RequiresRoles.class);
            assertTrue(rr.allRequired());
        }
    }

    @Nested
    @DisplayName("@Component (@Inherited)")
    class ComponentTests {
        @Test void component_name_readable() throws Exception {
            @Component(name = "myService")
            class Marked {}
            Component c = Marked.class.getAnnotation(Component.class);
            assertNotNull(c);
            assertEquals("myService", c.name());
        }

        @Test void component_default_name_is_empty() throws Exception {
            @Component
            class Marked {}
            Component c = Marked.class.getAnnotation(Component.class);
            assertEquals("", c.name());
        }
    }
}
