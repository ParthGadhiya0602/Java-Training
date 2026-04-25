package com.javatraining.security.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input validation is the primary defence against OWASP A03 (Injection).
 * Constraints are declared on the DTO, not scattered through service logic,
 * and enforced by @Valid in the controller - one place, always applied.
 */
public record RegisterRequest(

        @NotBlank(message = "Username must not be blank")
        String username,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}
