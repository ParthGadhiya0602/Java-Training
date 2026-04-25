package com.javatraining.annotations;

import com.javatraining.annotations.AnnotationDefinitions.*;

/**
 * Module 20 - Annotated sample classes used by tests and the processor.
 *
 * These classes demonstrate how the custom annotations from AnnotationDefinitions
 * are applied to real code so that RuntimeAnnotationProcessor can read them.
 */
public class AnnotationUsageExamples {

    // ── Service class with multiple annotations ───────────────────────────────

    @Author("Alice")
    @Tag("api")
    @Tag("public")
    @Component(name = "userService")
    public static class UserService {

        @Inject
        private Object repository;          // would normally be injected

        @Inject
        private Object eventBus;

        @RequiresRoles({"admin", "user"})
        public String listUsers() { return "[]"; }

        @RequiresRoles(value = {"admin"}, allRequired = true)
        public String deleteUser(String id) { return "deleted: " + id; }

        @Beta(reason = "experimental search")
        @Tag("search")
        public String searchUsers(String query) { return "results for: " + query; }

        public String getUser(String id) { return "user: " + id; }
    }

    // ── @Inherited demo ───────────────────────────────────────────────────────

    @Component(name = "baseRepo")
    public static class BaseRepository {
        public String find(String id) { return id; }
    }

    /** Does NOT re-declare @Component - inherits it from BaseRepository. */
    public static class UserRepository extends BaseRepository {
        public String findByName(String name) { return name; }
    }

    // ── @KnownIssue demo ──────────────────────────────────────────────────────

    @KnownIssue(description = "N+1 query issue", severity = KnownIssue.Severity.HIGH)
    public static class LegacyReportService {

        @KnownIssue(description = "no pagination support", severity = KnownIssue.Severity.MEDIUM)
        public String generateReport() { return "report"; }

        @KnownIssue(description = "minor formatting glitch", severity = KnownIssue.Severity.LOW)
        public String formatHeader() { return "header"; }

        public String cachedSummary() { return "summary"; }
    }

    // ── @Beta class ───────────────────────────────────────────────────────────

    @Beta(reason = "API may change")
    @Author(value = "Bob", date = "2024-01-15")
    public static class ExperimentalFeature {
        public String run() { return "experimental"; }
    }

    // ── @RequiresRoles on class ───────────────────────────────────────────────

    @RequiresRoles({"admin"})
    public static class AdminPanel {

        @RequiresRoles({"admin", "superuser"})
        public String nukeDatabase() { return "boom"; }

        @RequiresRoles({"admin"})
        public String viewLogs() { return "logs"; }
    }

    // ── Partially-injected object (for null-field check) ──────────────────────

    public static class PartiallyInjected {
        @Inject
        private Object serviceA = new Object();  // injected

        @Inject
        private Object serviceB;                 // NOT injected (null)

        @Inject
        private Object serviceC;                 // NOT injected (null)
    }
}
