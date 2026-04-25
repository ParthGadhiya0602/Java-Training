package com.javatraining.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies OWASP A01 (Broken Access Control) defences.
 *
 * @WithMockUser injects a synthetic SecurityContext - no real credentials are
 * submitted, and the UserDetailsService is not invoked. This isolates the access
 * control policy from the authentication mechanism.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccessControlTest {

    @Autowired MockMvc mockMvc;

    // ── A01: unauthenticated access ────────────────────────────────────────────

    @Test
    void unauthenticated_request_to_protected_endpoint_returns_401() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── A01: horizontal / vertical privilege escalation ───────────────────────

    @Test
    @WithMockUser(roles = "USER")
    void user_role_cannot_access_admin_endpoint_returns_403() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_role_can_access_admin_endpoint_returns_200() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk());
    }
}
