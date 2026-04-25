package com.javatraining.springsecurity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatraining.springsecurity.dto.ProductRequest;
import com.javatraining.springsecurity.model.Product;
import com.javatraining.springsecurity.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization tests using a full application context in a mock web environment.
 *
 * @SpringBootTest(MOCK) + @AutoConfigureMockMvc:
 *   - Full Spring context loads - our SecurityConfig, JwtUtil, JwtAuthenticationFilter all present
 *   - MockMvc is auto-configured (no real HTTP server)
 *   - Our SecurityFilterChain with CSRF disabled and custom AuthorizationManager is active
 *   - @EnableMethodSecurity is active - @PreAuthorize on controller methods is enforced
 *
 * @MockBean ProductService - the service is replaced with a Mockito mock;
 *   we only care about the security layer, not the business logic.
 *
 * @WithMockUser injects a synthetic Authentication into the SecurityContext,
 * bypassing the JWT filter. The JWT filter only acts when an Authorization header
 * is present; without it, the SecurityContext set by @WithMockUser is used as-is.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthorizationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductService productService;

    // ── Public endpoint (permitAll) ───────────────────────────────────────────

    @Test
    void anonymous_user_can_list_products() throws Exception {
        given(productService.findAll()).willReturn(List.of());

        // GET /api/products → permitAll() → no authentication required → 200
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    // ── Authentication required (anyRequest().authenticated()) ────────────────

    @Test
    void anonymous_user_cannot_get_product_by_id() throws Exception {
        // No auth, no JWT → SecurityContext empty → filter chain rejects → 401
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser   // default: role USER
    void authenticated_user_can_get_product_by_id() throws Exception {
        given(productService.findById(1L)).willReturn(
                new Product(1L, "Laptop", BigDecimal.valueOf(999), "Electronics"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    // ── @PreAuthorize("hasRole('ADMIN')") - POST ──────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void user_role_cannot_create_product() throws Exception {
        // @PreAuthorize("hasRole('ADMIN')") - ROLE_USER is not ROLE_ADMIN → 403
        ProductRequest req = new ProductRequest("Laptop", BigDecimal.TEN, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_role_can_create_product() throws Exception {
        given(productService.create(any())).willReturn(
                new Product(1L, "Laptop", BigDecimal.TEN, "Electronics"));

        ProductRequest req = new ProductRequest("Laptop", BigDecimal.TEN, "Electronics");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    // ── @PreAuthorize("hasRole('ADMIN')") - DELETE ────────────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void user_role_cannot_delete_product() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_role_can_delete_product() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
