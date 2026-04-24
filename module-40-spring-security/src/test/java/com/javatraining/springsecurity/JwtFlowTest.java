package com.javatraining.springsecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.springsecurity.dto.LoginRequest;
import com.javatraining.springsecurity.dto.ProductRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end JWT flow tests — full Spring context with MockMvc.
 *
 * No @MockBean: the real ProductService (in-memory store) is used so we can
 * test the full create → read cycle. Each test class gets its own Spring context
 * because AuthorizationTest uses @MockBean ProductService, which would change the
 * context fingerprint and force a new context here anyway.
 *
 * We use MockMvc (not TestRestTemplate) to avoid Apache HttpClient's
 * automatic auth-retry on 401 responses (NonRepeatableRequestException).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class JwtFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_with_valid_credentials_returns_jwt_token() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("token").asText();
        // JWT format: header.payload.signature — three dot-separated base64url segments
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void login_with_wrong_password_returns_401_problem_detail() throws Exception {
        // AuthController.login() throws BadCredentialsException → GlobalExceptionHandler
        // returns ProblemDetail with 401. MockMvc sees the response directly — no HTTP
        // client auth-retry behavior.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user", "WRONG_PASSWORD"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── Authentication via JWT ────────────────────────────────────────────────

    @Test
    void request_without_token_returns_401() throws Exception {
        // No Authorization header → SecurityContext empty → filter chain rejects → 401
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_with_invalid_token_returns_401() throws Exception {
        // "not.a.valid.jwt" fails JwtUtil.parseClaims() → exception caught in filter →
        // SecurityContext stays empty → 401
        mockMvc.perform(get("/api/products/1")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void valid_user_jwt_accesses_protected_endpoint() throws Exception {
        // Login → get token → use token on protected endpoint
        String token = loginAndGetToken("user", "password");

        // GET /api/products/999 — no such product, but 404 (not 401) proves JWT validated
        mockMvc.perform(get("/api/products/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── Role-based access via JWT ─────────────────────────────────────────────

    @Test
    void user_jwt_cannot_create_product_returns_403() throws Exception {
        String userToken = loginAndGetToken("user", "password");
        ProductRequest req = new ProductRequest("Laptop", BigDecimal.valueOf(999), "Electronics");

        // ROLE_USER is authenticated → passes filter chain
        // @PreAuthorize("hasRole('ADMIN')") fails → 403
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_jwt_creates_product_and_user_jwt_reads_it() throws Exception {
        // Admin creates a product
        String adminToken = loginAndGetToken("admin", "admin123");
        ProductRequest req = new ProductRequest("Keyboard", BigDecimal.valueOf(149), "Accessories");

        String createBody = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(createBody).get("id").asLong();

        // A regular user can read the product using their own JWT
        String userToken = loginAndGetToken("user", "password");
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.category").value("Accessories"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
