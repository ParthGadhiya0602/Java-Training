package com.javatraining.security.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        // In a real app: look up and delete the user.
        // Access to this endpoint is restricted by SecurityConfig: hasRole("ADMIN").
        // A USER-role request never reaches this method - Spring Security rejects it
        // with 403 Forbidden before the DispatcherServlet processes the request.
        return Map.of("deleted", id.toString());
    }
}
