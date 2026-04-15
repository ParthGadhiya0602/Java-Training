package com.javatraining.controlflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Conditionals — if/else correctness")
class ConditionalTest {

    @ParameterizedTest(name = "score={0} → grade={1}")
    @CsvSource({
        "100, A",
        "90,  A",
        "89,  B",
        "80,  B",
        "79,  C",
        "70,  C",
        "69,  D",
        "60,  D",
        "59,  F",
        "0,   F",
    })
    @DisplayName("Grade classification covers all boundary values")
    void gradeClassification(int score, String expected) {
        assertEquals(expected, ConditionalDemo.classify(score));
    }

    @Test
    @DisplayName("Score below 0 throws IllegalArgumentException")
    void invalidScoreLow() {
        assertThrows(IllegalArgumentException.class,
            () -> ConditionalDemo.classify(-1));
    }

    @Test
    @DisplayName("Score above 100 throws IllegalArgumentException")
    void invalidScoreHigh() {
        assertThrows(IllegalArgumentException.class,
            () -> ConditionalDemo.classify(101));
    }

    @Test
    @DisplayName("safeGreeting handles null name without NPE")
    void safeGreetingNull() {
        assertEquals("Hello, stranger!", ConditionalDemo.safeGreeting(null));
    }

    @Test
    @DisplayName("safeGreeting uses provided name when non-null")
    void safeGreetingNonNull() {
        assertEquals("Hello, Parth!", ConditionalDemo.safeGreeting("Parth"));
    }
}
