package com.javatraining.jpa;

import com.javatraining.jpa.entity.Author;
import com.javatraining.jpa.entity.Book;
import com.javatraining.jpa.entity.BookDetail;
import com.javatraining.jpa.entity.Tag;
import org.junit.jupiter.api.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates Bean Validation 3.0 (Jakarta) with Hibernate Validator.
 *
 * <p>Validation runs at the application layer - independently of the database.
 * The same {@link Validator} instance is used in REST controller layers,
 * service methods, and is also invoked automatically by Hibernate before
 * flushing entities to the DB.
 *
 * <p>No database or EntityManager is needed here - just a {@link Validator}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidationTest {

    private Validator validator;

    @BeforeAll
    void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ── @NotBlank ────────────────────────────────────────────────────────────

    @Test
    void blank_author_name_fails_not_blank_constraint() {
        Author author = new Author("", "valid@test.com");
        Set<ConstraintViolation<Author>> violations = validator.validate(author);
        assertFalse(violations.isEmpty(), "Blank name should produce a violation");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")),
                "Violation is on the 'name' field");
    }

    @Test
    void blank_tag_name_fails_not_blank_constraint() {
        Tag tag = new Tag("   "); // whitespace-only → @NotBlank fails
        Set<ConstraintViolation<Tag>> violations = validator.validate(tag);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    // ── @Email ───────────────────────────────────────────────────────────────

    @Test
    void invalid_email_fails_email_constraint() {
        Author author = new Author("Valid Name", "not-an-email");
        Set<ConstraintViolation<Author>> violations = validator.validate(author);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")),
                "Invalid e-mail should produce a violation on 'email'");
    }

    // ── @DecimalMin ───────────────────────────────────────────────────────────

    @Test
    void negative_book_price_fails_decimal_min_constraint() {
        Book book = new Book("Valid Title", new BigDecimal("-0.01"));
        Set<ConstraintViolation<Book>> violations = validator.validate(book);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("price")),
                "Negative price should produce a violation on 'price'");
    }

    // ── @Min ─────────────────────────────────────────────────────────────────

    @Test
    void page_count_of_zero_fails_min_constraint() {
        BookDetail detail = new BookDetail("978-1-000-0000-0", 0, "synopsis");
        Set<ConstraintViolation<BookDetail>> violations = validator.validate(detail);
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("pageCount")));
    }

    // ── valid entity ─────────────────────────────────────────────────────────

    @Test
    void valid_entities_produce_no_constraint_violations() {
        Author author = new Author("Valid Author", "valid@example.com");
        assertEquals(0, validator.validate(author).size(), "Author should be valid");

        Book book = new Book("Valid Book", new BigDecimal("9.99"));
        assertEquals(0, validator.validate(book).size(), "Book should be valid");

        BookDetail detail = new BookDetail("978-0-000-0000-0", 100, "synopsis");
        assertEquals(0, validator.validate(detail).size(), "BookDetail should be valid");
    }
}
