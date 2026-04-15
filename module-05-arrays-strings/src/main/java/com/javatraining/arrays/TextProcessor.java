package com.javatraining.arrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PRACTICAL EXERCISE: CSV Text Processor
 *
 * Parses, validates, and reports on user records supplied as CSV strings.
 * Every concept from this module is used in a realistic context:
 *
 *   ARRAYS:
 *     - split() produces String[] for each CSV row
 *     - Arrays.sort() sorts records by field
 *     - Arrays.copyOfRange() extracts a page of records
 *
 *   STRINGS:
 *     - String methods: strip(), split(), isEmpty(), format()
 *     - StringBuilder for building the summary report
 *     - String.join() for CSV output
 *     - Text block for multi-line templates
 *
 *   REGEX:
 *     - Pre-compiled Pattern objects for field validation
 *     - Matcher.find() for extracting phone numbers
 *     - Named groups for structured date parsing
 *     - replaceAll() for normalising input data
 */
public class TextProcessor {

    // -------------------------------------------------------------------------
    // Pre-compiled patterns — static final, compiled once
    // -------------------------------------------------------------------------
    private static final Pattern VALID_NAME  = Pattern.compile("^[A-Za-z]+(\\s[A-Za-z]+)*$");
    private static final Pattern VALID_EMAIL = Pattern.compile(
        "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern VALID_PHONE = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern VALID_AGE   = Pattern.compile("^\\d{1,3}$");
    private static final Pattern DATE_FORMAT = Pattern.compile(
        "^(?<year>\\d{4})-(?<month>0[1-9]|1[0-2])-(?<day>0[1-9]|[12]\\d|3[01])$");
    private static final Pattern EXTRA_SPACE = Pattern.compile("\\s{2,}");

    // -------------------------------------------------------------------------
    // Record to hold one parsed and validated user row
    // -------------------------------------------------------------------------
    record UserRecord(
        int    rowNumber,
        String name,
        String email,
        String phone,
        int    age,
        String joinDate,
        List<String> validationErrors
    ) {
        boolean isValid() { return validationErrors.isEmpty(); }

        String toCsv() {
            return String.join(",", name, email, phone,
                String.valueOf(age), joinDate);
        }
    }

    // -------------------------------------------------------------------------
    // Parse and validate a single CSV row
    // -------------------------------------------------------------------------
    static UserRecord parseRow(int rowNumber, String rawLine) {
        List<String> errors = new ArrayList<>();

        // Normalise: strip outer whitespace and collapse internal runs
        String line = EXTRA_SPACE.matcher(rawLine.strip()).replaceAll(" ");

        // Split on comma, trim each field
        String[] fields = line.split(",", -1);   // -1 keeps trailing empty fields
        for (int i = 0; i < fields.length; i++) fields[i] = fields[i].strip();

        // Expect exactly 5 fields: name, email, phone, age, joinDate
        if (fields.length != 5) {
            errors.add("Expected 5 fields, got " + fields.length);
            return new UserRecord(rowNumber, rawLine, "", "", 0, "", errors);
        }

        String name     = fields[0];
        String email    = fields[1];
        String phone    = fields[2];
        String ageStr   = fields[3];
        String joinDate = fields[4];

        // Validate name
        if (name.isEmpty())
            errors.add("Name is empty");
        else if (!VALID_NAME.matcher(name).matches())
            errors.add("Name contains invalid characters: '" + name + "'");

        // Validate email
        if (!VALID_EMAIL.matcher(email).matches())
            errors.add("Invalid email: '" + email + "'");

        // Validate phone — strip spaces/dashes before checking
        String normalPhone = phone.replaceAll("[\\s\\-]", "");
        if (!VALID_PHONE.matcher(normalPhone).matches())
            errors.add("Invalid phone: '" + phone + "'");

        // Validate age
        int age = 0;
        if (!VALID_AGE.matcher(ageStr).matches()) {
            errors.add("Invalid age: '" + ageStr + "'");
        } else {
            age = Integer.parseInt(ageStr);
            if (age < 18 || age > 120)
                errors.add("Age out of range: " + age);
        }

        // Validate join date
        if (!DATE_FORMAT.matcher(joinDate).matches())
            errors.add("Invalid date format (expected YYYY-MM-DD): '" + joinDate + "'");

        return new UserRecord(rowNumber, name, email, normalPhone, age, joinDate, errors);
    }

    // -------------------------------------------------------------------------
    // Parse a block of CSV text (header + rows)
    // -------------------------------------------------------------------------
    static List<UserRecord> parseCsv(String csvText) {
        String[] lines = csvText.strip().split("\\R"); // \R matches any line ending
        List<UserRecord> records = new ArrayList<>();

        // Skip header line (index 0)
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].strip();
            if (!line.isEmpty())
                records.add(parseRow(i, line));
        }
        return records;
    }

    // -------------------------------------------------------------------------
    // Build summary report using StringBuilder + Arrays
    // -------------------------------------------------------------------------
    static String buildReport(List<UserRecord> records) {
        long valid   = records.stream().filter(UserRecord::isValid).count();
        long invalid = records.size() - valid;

        // Collect valid records and sort by name using Arrays.sort
        UserRecord[] validArr = records.stream()
            .filter(UserRecord::isValid)
            .toArray(UserRecord[]::new);
        Arrays.sort(validArr, (a, b) -> a.name().compareToIgnoreCase(b.name()));

        StringBuilder sb = new StringBuilder();

        // Text-block style header template
        sb.append("""
                ╔══════════════════════════════════════════════════╗
                ║           CSV PROCESSING REPORT                  ║
                ╚══════════════════════════════════════════════════╝
                """);

        sb.append(String.format("  Total rows  : %d%n", records.size()));
        sb.append(String.format("  Valid        : %d%n", valid));
        sb.append(String.format("  Invalid      : %d%n%n", invalid));

        // Valid records — sorted by name
        if (validArr.length > 0) {
            sb.append("  VALID RECORDS (sorted by name):\n");
            sb.append(String.format("  %-20s %-28s %-12s %3s  %-10s%n",
                "Name", "Email", "Phone", "Age", "Join Date"));
            sb.append("  " + "─".repeat(80) + "\n");

            for (UserRecord r : validArr) {
                sb.append(String.format("  %-20s %-28s %-12s %3d  %-10s%n",
                    r.name(), r.email(), r.phone(), r.age(), r.joinDate()));
            }
        }

        // Invalid records with error details
        List<UserRecord> invalidList = records.stream()
            .filter(r -> !r.isValid()).toList();
        if (!invalidList.isEmpty()) {
            sb.append("\n  INVALID RECORDS:\n");
            for (UserRecord r : invalidList) {
                sb.append(String.format("  Row %-3d : %s%n", r.rowNumber(), r.name()));
                for (String err : r.validationErrors())
                    sb.append(String.format("           → %s%n", err));
            }
        }

        // Age statistics using arrays
        if (validArr.length > 0) {
            int[] ages = Arrays.stream(validArr)
                .mapToInt(UserRecord::age).toArray();
            Arrays.sort(ages);
            double avgAge = Arrays.stream(ages).average().orElse(0);

            sb.append(String.format("%n  AGE STATS: min=%d  max=%d  avg=%.1f%n",
                ages[0], ages[ages.length - 1], avgAge));
        }

        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Pagination: extract a page of sorted records using Arrays.copyOfRange
    // -------------------------------------------------------------------------
    static List<UserRecord> getPage(List<UserRecord> records,
                                    int pageNumber, int pageSize) {
        UserRecord[] arr = records.stream()
            .filter(UserRecord::isValid)
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toArray(UserRecord[]::new);

        int from = (pageNumber - 1) * pageSize;
        int to   = Math.min(from + pageSize, arr.length);

        if (from >= arr.length) return List.of();
        return Arrays.asList(Arrays.copyOfRange(arr, from, to));
    }

    // -------------------------------------------------------------------------
    // Extract all emails from free text using regex
    // -------------------------------------------------------------------------
    private static final Pattern EMAIL_IN_TEXT = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    static List<String> extractEmails(String text) {
        List<String> result = new ArrayList<>();
        Matcher m = EMAIL_IN_TEXT.matcher(text);
        while (m.find()) result.add(m.group());
        return result;
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        String csv = """
                name,email,phone,age,joinDate
                Alice Sharma,alice@example.com,9876543210,28,2022-03-15
                Bob Patel,bob@company.org,8765432109,35,2019-11-01
                carol,,7654321098,17,2023-07-22
                David Mehta,david@invalid,6543210987,45,2021-05-30
                Eve Singh,eve@domain.co.in,9123456789,31,2020-08-14
                Frank  Kumar,frank@test.com,8901234567,29,2024-01-10
                Grace,grace@web.net,7890123456,abc,2023-12-25
                Henry D'Souza,henry@corp.in,9012345678,42,2018-06-05
                """;

        List<UserRecord> records = parseCsv(csv);
        System.out.println(buildReport(records));

        System.out.println("=== Pagination (page 1, size 3) ===");
        List<UserRecord> page1 = getPage(records, 1, 3);
        page1.forEach(r -> System.out.println("  " + r.name() + " | " + r.email()));

        System.out.println("\n=== Email extraction from free text ===");
        String text = "Please contact support@help.com or sales@company.org for pricing.";
        System.out.println("Found: " + extractEmails(text));

        System.out.println("\n=== Date parsing with named groups ===");
        String[] dates = {"2024-04-15", "2023-12-31", "invalid-date"};
        for (String d : dates) {
            Matcher m = DATE_FORMAT.matcher(d);
            if (m.matches()) {
                System.out.printf("  %s → year=%s month=%s day=%s%n",
                    d, m.group("year"), m.group("month"), m.group("day"));
            } else {
                System.out.println("  " + d + " → invalid format");
            }
        }
    }
}
