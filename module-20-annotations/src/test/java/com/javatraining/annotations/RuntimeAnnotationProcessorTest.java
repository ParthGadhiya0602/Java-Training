package com.javatraining.annotations;

import com.javatraining.annotations.AnnotationDefinitions.KnownIssue;
import com.javatraining.annotations.AnnotationUsageExamples.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuntimeAnnotationProcessor")
class RuntimeAnnotationProcessorTest {

    @Nested
    @DisplayName("getAuthor")
    class GetAuthor {
        @Test void reads_author_from_annotated_class() {
            assertEquals("Alice", RuntimeAnnotationProcessor.getAuthor(UserService.class));
        }

        @Test void returns_unknown_when_no_author() {
            assertEquals("unknown", RuntimeAnnotationProcessor.getAuthor(AdminPanel.class));
        }
    }

    @Nested
    @DisplayName("getTags (repeatable)")
    class GetTags {
        @Test void reads_multiple_tags_from_class() {
            List<String> tags = RuntimeAnnotationProcessor.getTags(UserService.class);
            assertTrue(tags.contains("api"));
            assertTrue(tags.contains("public"));
        }

        @Test void returns_empty_list_when_no_tags() {
            List<String> tags = RuntimeAnnotationProcessor.getTags(AdminPanel.class);
            assertTrue(tags.isEmpty());
        }

        @Test void reads_tag_from_method() throws Exception {
            var method = UserService.class.getDeclaredMethod("searchUsers", String.class);
            List<String> tags = RuntimeAnnotationProcessor.getMethodTags(method);
            assertTrue(tags.contains("search"));
        }
    }

    @Nested
    @DisplayName("isBeta")
    class IsBeta {
        @Test void annotated_class_is_beta() {
            assertTrue(RuntimeAnnotationProcessor.isBeta(ExperimentalFeature.class));
        }

        @Test void non_annotated_class_is_not_beta() {
            assertFalse(RuntimeAnnotationProcessor.isBeta(AdminPanel.class));
        }

        @Test void beta_method_detected() throws Exception {
            var method = UserService.class.getDeclaredMethod("searchUsers", String.class);
            assertTrue(RuntimeAnnotationProcessor.isMethodBeta(method));
        }

        @Test void non_beta_method_detected() throws Exception {
            var method = UserService.class.getDeclaredMethod("listUsers");
            assertFalse(RuntimeAnnotationProcessor.isMethodBeta(method));
        }
    }

    @Nested
    @DisplayName("getRequiredRoles")
    class GetRequiredRoles {
        @Test void finds_roles_for_annotated_methods() {
            Map<String, List<String>> roles =
                RuntimeAnnotationProcessor.getRequiredRoles(UserService.class);
            assertTrue(roles.containsKey("listUsers"));
            assertTrue(roles.get("listUsers").contains("admin"));
            assertTrue(roles.get("listUsers").contains("user"));
        }

        @Test void non_annotated_method_not_in_map() {
            Map<String, List<String>> roles =
                RuntimeAnnotationProcessor.getRequiredRoles(UserService.class);
            assertFalse(roles.containsKey("getUser"));
        }

        @Test void admin_only_method_has_one_role() {
            Map<String, List<String>> roles =
                RuntimeAnnotationProcessor.getRequiredRoles(UserService.class);
            assertEquals(List.of("admin"), roles.get("deleteUser"));
        }
    }

    @Nested
    @DisplayName("getInjectableFields")
    class GetInjectableFields {
        @Test void finds_two_inject_fields_in_user_service() {
            List<String> fields =
                RuntimeAnnotationProcessor.getInjectableFields(UserService.class);
            assertEquals(2, fields.size());
            assertTrue(fields.contains("repository"));
            assertTrue(fields.contains("eventBus"));
        }

        @Test void class_with_no_inject_returns_empty_list() {
            List<String> fields =
                RuntimeAnnotationProcessor.getInjectableFields(AdminPanel.class);
            assertTrue(fields.isEmpty());
        }
    }

    @Nested
    @DisplayName("findNullInjectedFields")
    class FindNullInjectedFields {
        @Test void detects_null_injected_fields() throws Exception {
            PartiallyInjected obj = new PartiallyInjected();
            List<String> nullFields =
                RuntimeAnnotationProcessor.findNullInjectedFields(obj);
            assertEquals(2, nullFields.size());
            assertTrue(nullFields.contains("serviceB"));
            assertTrue(nullFields.contains("serviceC"));
        }

        @Test void non_null_field_not_in_result() throws Exception {
            PartiallyInjected obj = new PartiallyInjected();
            List<String> nullFields =
                RuntimeAnnotationProcessor.findNullInjectedFields(obj);
            assertFalse(nullFields.contains("serviceA"));
        }
    }

    @Nested
    @DisplayName("@Inherited — hasComponent")
    class InheritedComponent {
        @Test void base_class_has_component() {
            assertTrue(RuntimeAnnotationProcessor.hasComponent(BaseRepository.class));
        }

        @Test void subclass_inherits_component() {
            assertTrue(RuntimeAnnotationProcessor.hasComponent(UserRepository.class));
        }

        @Test void component_name_from_base() {
            assertEquals("baseRepo",
                RuntimeAnnotationProcessor.getComponentName(BaseRepository.class));
        }

        @Test void subclass_inherits_component_name() {
            assertEquals("baseRepo",
                RuntimeAnnotationProcessor.getComponentName(UserRepository.class));
        }
    }

    @Nested
    @DisplayName("getKnownIssues")
    class GetKnownIssues {
        @Test void finds_high_severity_class_issue() {
            List<String> issues = RuntimeAnnotationProcessor.getKnownIssues(
                LegacyReportService.class, KnownIssue.Severity.HIGH);
            assertEquals(1, issues.size());
            assertTrue(issues.get(0).contains("N+1 query"));
        }

        @Test void finds_all_issues_at_low_threshold() {
            List<String> issues = RuntimeAnnotationProcessor.getKnownIssues(
                LegacyReportService.class, KnownIssue.Severity.LOW);
            assertEquals(3, issues.size());
        }

        @Test void medium_threshold_excludes_low() {
            List<String> issues = RuntimeAnnotationProcessor.getKnownIssues(
                LegacyReportService.class, KnownIssue.Severity.MEDIUM);
            assertTrue(issues.stream().noneMatch(s -> s.contains("formatting glitch")));
        }
    }

    @Nested
    @DisplayName("describeAnnotations")
    class DescribeAnnotations {
        @Test void report_contains_class_name() {
            String report = RuntimeAnnotationProcessor.describeAnnotations(UserService.class);
            assertTrue(report.contains("UserService"));
        }

        @Test void report_contains_class_annotation() {
            String report = RuntimeAnnotationProcessor.describeAnnotations(UserService.class);
            assertTrue(report.contains("Author") || report.contains("Component"));
        }
    }
}
