package com.javatraining.security.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * @Valid triggers Bean Validation on the request body.
     * If any constraint fails, Spring throws MethodArgumentNotValidException
     * which the default error handler maps to HTTP 400 Bad Request.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        // In a real app: hash password, persist user, return 201.
        // The focus here is the validation layer - @Valid enforces constraints
        // before this method body is ever reached.
        return Map.of("username", request.username(), "message", "registered");
    }
}
