package com.javatraining.arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Regex — validation, extraction, transformation")
class RegexTest {

    @ParameterizedTest(name = "{0} is a valid email")
    @ValueSource(strings = {
        "user@example.com",
        "user.name+tag@domain.co.in",
        "user-name@sub.domain.org",
        "u@d.io",
    })
    void validEmails(String email) {
        assertTrue(RegexDemo.isValidEmail(email));
    }

    @ParameterizedTest(name = "{0} is NOT a valid email")
    @ValueSource(strings = {
        "notanemail",
        "@nodomain.com",
        "user@",
        "user @domain.com",
        "",
    })
    void invalidEmails(String email) {
        assertFalse(RegexDemo.isValidEmail(email));
    }

    @ParameterizedTest(name = "{0} is a valid Indian mobile")
    @ValueSource(strings = {"9876543210", "8765432109", "7654321098", "6543210987"})
    void validMobiles(String mobile) {
        assertTrue(RegexDemo.isValidMobile(mobile));
    }

    @ParameterizedTest(name = "{0} is NOT a valid Indian mobile")
    @ValueSource(strings = {"1234567890", "98765", "99999999999", ""})
    void invalidMobiles(String mobile) {
        assertFalse(RegexDemo.isValidMobile(mobile));
    }

    @Test
    @DisplayName("isInteger accepts valid integers including negative")
    void integers() {
        assertTrue(RegexDemo.isInteger("42"));
        assertTrue(RegexDemo.isInteger("-100"));
        assertTrue(RegexDemo.isInteger("0"));
        assertFalse(RegexDemo.isInteger("3.14"));
        assertFalse(RegexDemo.isInteger("abc"));
    }

    @Test
    @DisplayName("extractUrls finds all http/https URLs")
    void extractUrls() {
        String text = "See https://java.com and http://docs.oracle.com for details. " +
                      "Also ftp://skip.me should not match.";
        List<String> urls = RegexDemo.extractUrls(text);
        assertEquals(2, urls.size());
        assertTrue(urls.stream().anyMatch(u -> u.contains("java.com")));
        assertTrue(urls.stream().anyMatch(u -> u.contains("docs.oracle.com")));
    }

    @Test
    @DisplayName("normaliseWhitespace collapses multiple spaces to one")
    void normaliseWhitespace() {
        assertEquals("hello world test",
            RegexDemo.normaliseWhitespace("  hello   world\t  test  "));
    }

    @Test
    @DisplayName("stripHtml removes all HTML tags")
    void stripHtml() {
        assertEquals("Hello World !",
            RegexDemo.stripHtml("<h1>Hello</h1> <p>World <b>!</b></p>"));
    }
}
