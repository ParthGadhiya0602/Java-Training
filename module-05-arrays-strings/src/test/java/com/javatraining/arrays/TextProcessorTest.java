package com.javatraining.arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextProcessor - CSV parsing and validation")
class TextProcessorTest {

    private static final String VALID_CSV = """
            name,email,phone,age,joinDate
            Alice Sharma,alice@example.com,9876543210,28,2022-03-15
            Bob Patel,bob@company.org,8765432109,35,2019-11-01
            Carol Singh,carol@web.in,7654321098,31,2023-07-22
            """;

    @Nested
    @DisplayName("parseRow - individual row parsing")
    class ParseRowTests {

        @Test
        @DisplayName("Valid row produces a valid record with correct fields")
        void validRow() {
            var r = TextProcessor.parseRow(1, "Alice Sharma,alice@example.com,9876543210,28,2022-03-15");
            assertTrue(r.isValid());
            assertEquals("Alice Sharma", r.name());
            assertEquals("alice@example.com", r.email());
            assertEquals("9876543210", r.phone());
            assertEquals(28, r.age());
            assertEquals("2022-03-15", r.joinDate());
        }

        @Test
        @DisplayName("Wrong field count marks record invalid")
        void wrongFieldCount() {
            var r = TextProcessor.parseRow(1, "Alice,alice@example.com,9876543210");
            assertFalse(r.isValid());
            assertTrue(r.validationErrors().get(0).contains("Expected 5 fields"));
        }

        @Test
        @DisplayName("Invalid email is caught")
        void invalidEmail() {
            var r = TextProcessor.parseRow(1, "Alice,not-an-email,9876543210,25,2022-01-01");
            assertFalse(r.isValid());
            assertTrue(r.validationErrors().stream()
                .anyMatch(e -> e.contains("email")));
        }

        @Test
        @DisplayName("Invalid phone is caught")
        void invalidPhone() {
            var r = TextProcessor.parseRow(1, "Alice,a@b.com,1234567890,25,2022-01-01");
            assertFalse(r.isValid());
            assertTrue(r.validationErrors().stream()
                .anyMatch(e -> e.contains("phone")));
        }

        @Test
        @DisplayName("Underage (< 18) is caught")
        void underage() {
            var r = TextProcessor.parseRow(1, "Alice,a@b.com,9876543210,17,2022-01-01");
            assertFalse(r.isValid());
            assertTrue(r.validationErrors().stream()
                .anyMatch(e -> e.contains("Age out of range")));
        }

        @Test
        @DisplayName("Invalid date format is caught")
        void invalidDate() {
            var r = TextProcessor.parseRow(1, "Alice,a@b.com,9876543210,25,15-04-2022");
            assertFalse(r.isValid());
            assertTrue(r.validationErrors().stream()
                .anyMatch(e -> e.contains("date")));
        }

        @Test
        @DisplayName("Phone with spaces/dashes is normalised before validation")
        void phoneNormalisation() {
            var r = TextProcessor.parseRow(1, "Alice,a@b.com,98765 43210,25,2022-01-01");
            assertTrue(r.isValid(), "Phone with space should pass after normalisation");
            assertEquals("9876543210", r.phone());
        }

        @Test
        @DisplayName("Extra spaces around fields are trimmed")
        void fieldTrimming() {
            var r = TextProcessor.parseRow(1, "  Alice Sharma , alice@example.com , 9876543210 , 28 , 2022-03-15 ");
            assertTrue(r.isValid());
            assertEquals("Alice Sharma", r.name());
        }
    }

    @Nested
    @DisplayName("parseCsv - full CSV block")
    class ParseCsvTests {

        @Test
        @DisplayName("Parses correct number of data rows (header excluded)")
        void rowCount() {
            List<TextProcessor.UserRecord> records = TextProcessor.parseCsv(VALID_CSV);
            assertEquals(3, records.size());
        }

        @Test
        @DisplayName("All rows in valid CSV are valid")
        void allValid() {
            List<TextProcessor.UserRecord> records = TextProcessor.parseCsv(VALID_CSV);
            assertTrue(records.stream().allMatch(TextProcessor.UserRecord::isValid));
        }
    }

    @Nested
    @DisplayName("getPage - pagination of valid records")
    class PaginationTests {

        @Test
        @DisplayName("Page 1 returns first N records sorted by name")
        void page1() {
            List<TextProcessor.UserRecord> records = TextProcessor.parseCsv(VALID_CSV);
            List<TextProcessor.UserRecord> page = TextProcessor.getPage(records, 1, 2);
            assertEquals(2, page.size());
            // Should be alphabetically: Alice first, Bob second
            assertEquals("Alice Sharma", page.get(0).name());
            assertEquals("Bob Patel",    page.get(1).name());
        }

        @Test
        @DisplayName("Page beyond total returns empty list")
        void pageOutOfBounds() {
            List<TextProcessor.UserRecord> records = TextProcessor.parseCsv(VALID_CSV);
            List<TextProcessor.UserRecord> page = TextProcessor.getPage(records, 99, 10);
            assertTrue(page.isEmpty());
        }
    }

    @Nested
    @DisplayName("extractEmails - regex-based extraction")
    class ExtractEmailTests {

        @Test
        @DisplayName("Extracts all valid emails from free text")
        void extractFromText() {
            String text = "Contact support@help.com or sales@company.org - both are monitored.";
            List<String> emails = TextProcessor.extractEmails(text);
            assertEquals(2, emails.size());
            assertTrue(emails.contains("support@help.com"));
            assertTrue(emails.contains("sales@company.org"));
        }

        @Test
        @DisplayName("Returns empty list when no emails found")
        void noEmails() {
            assertTrue(TextProcessor.extractEmails("No emails here.").isEmpty());
        }
    }
}
