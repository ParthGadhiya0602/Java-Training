package com.javatraining.cleancode.smells;

import java.time.LocalDate;
import java.util.List;

/**
 * Refactoring: Extract Method to eliminate Duplicate Code.
 *
 * <p><strong>Smell — duplicated code:</strong>
 * <pre>
 *   String generatePdf() {
 *       String header = "=== Report — " + LocalDate.now() + " ===";  // duplicated
 *       String footer = "=== END ===";                                 // duplicated
 *       ...
 *   }
 *   String generateCsv() {
 *       String header = "=== Report — " + LocalDate.now() + " ===";  // same header
 *       String footer = "=== END ===";                                 // same footer
 *       ...
 *   }
 * </pre>
 *
 * <p><strong>Fix — Extract Method:</strong>
 * Private helper methods {@code buildHeader()} and {@code buildFooter()} are
 * defined once.  Changing the format requires editing one place only.
 *
 * <p>Also demonstrates: Magic Number smell → named constants.
 * {@code items.size() > 100} becomes {@code items.size() > MAX_ITEMS_PER_PAGE}.
 */
public class ReportBuilder {

    private static final int MAX_ITEMS_PER_PAGE = 100;
    private static final String SEPARATOR = "─".repeat(40);

    private final String title;

    public ReportBuilder(String title) {
        this.title = title;
    }

    public String generateTextReport(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader()).append("\n");
        sb.append(SEPARATOR).append("\n");
        items.forEach(item -> sb.append("  • ").append(item).append("\n"));
        if (items.size() > MAX_ITEMS_PER_PAGE)
            sb.append("  ... and ").append(items.size() - MAX_ITEMS_PER_PAGE).append(" more\n");
        sb.append(SEPARATOR).append("\n");
        sb.append(buildFooter(items.size()));
        return sb.toString();
    }

    public String generateCsvReport(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildHeader()).append("\n");
        sb.append("item\n");                  // CSV header row
        items.forEach(item -> sb.append(item).append("\n"));
        sb.append(buildFooter(items.size()));
        return sb.toString();
    }

    public String generateSummary(List<String> items) {
        return buildHeader() + "\n"
               + "Total items: " + items.size() + "\n"
               + buildFooter(items.size());
    }

    // ── private helpers — extracted once, shared by all generators ────────────

    private String buildHeader() {
        return "=== " + title + " — " + LocalDate.now() + " ===";
    }

    private String buildFooter(int itemCount) {
        return "=== END (" + itemCount + " items) ===";
    }
}
