package com.javatraining.springsecurity.config;

import com.javatraining.springsecurity.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
// @EnableMethodSecurity activates @PreAuthorize, @PostAuthorize, @Secured on any Spring bean
@EnableMethodSecurity
public class SecurityConfig {

    // Main filter chain — defines the HTTP security rules for this application
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                // Disable CSRF — stateless JWT APIs don't use cookies, so CSRF doesn't apply
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless — no HttpSession; every request must carry its own credentials
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules — evaluated top-to-bottom, first match wins
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()           // login endpoint
                        .requestMatchers(HttpMethod.GET, "/api/products").permitAll() // public list
                        .anyRequest().authenticated()                          // everything else
                )

                // Disable HTTP Basic — prevents "WWW-Authenticate: Basic" header on 401 responses,
                // which would cause Apache HttpClient to retry POST requests (non-repeatable stream).
                .httpBasic(AbstractHttpConfigurer::disable)

                // Return 401 JSON for unauthenticated requests instead of a redirect to /login
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                // Insert JWT filter before Spring's own username/password filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Two in-memory users: "user" (ROLE_USER) and "admin" (ROLE_ADMIN)
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.withUsername("user")
                .password(encoder.encode("password"))
                .roles("USER")
                .build();
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, admin);
    }

    // BCrypt: adaptive one-way hash — deliberately slow to resist brute-force attacks
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Exposes the AuthenticationManager so AuthController can call authenticate()
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
