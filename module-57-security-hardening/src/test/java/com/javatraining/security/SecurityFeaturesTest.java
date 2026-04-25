package com.javatraining.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies security headers (A05: Security Misconfiguration) and
 * input validation (A03: Injection prevention).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFeaturesTest {

    @Autowired MockMvc mockMvc;

    // ── A05: security headers present on every response ───────────────────────

    @Test
    void security_headers_are_present_on_public_endpoint() throws Exception {
        mockMvc.perform(get("/api/public/info"))
                .andExpect(status().isOk())
                // Prevents MIME-type sniffing attacks
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                // Prevents the page from being framed (clickjacking)
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    // ── A03: injection prevention via input validation ────────────────────────

    @Test
    void registration_with_blank_username_returns_400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":"password123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
