package com.javatraining.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration demonstrating OWASP defences:
 *
 *  A01 Broken Access Control    → role-based path restrictions
 *  A02 Cryptographic Failures   → BCrypt for password hashing (work factor 12)
 *  A05 Security Misconfiguration → security headers enabled, HTTPS enforced in prod
 *  A07 Identification Failures  → unauthenticated requests rejected with 401
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                // CSRF disabled for this stateless demo API.
                // Session-based apps (Thymeleaf, MVC forms) must keep CSRF enabled.
                .csrf(AbstractHttpConfigurer::disable)
                // Security headers — all defaults enabled:
                //   X-Content-Type-Options: nosniff      (prevents MIME-type sniffing)
                //   X-Frame-Options: DENY                (clickjacking protection)
                //   X-XSS-Protection: 0                  (disabled; rely on CSP instead)
                //   Cache-Control: no-store              (prevents sensitive data caching)
                //   Strict-Transport-Security            (HTTPS only, added for HTTPS requests)
                .headers(Customizer.withDefaults())
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 — ~300ms per hash, making brute-force expensive.
        // Use PasswordEncoderFactories.createDelegatingPasswordEncoder() if you need
        // to migrate from an older scheme (MD5, SHA-1) without forcing a password reset.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        // In-memory users for demo purposes.
        // In production, load from a database via JdbcUserDetailsManager or a
        // custom UserDetailsService backed by your user repository.
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username("user")
                        .password(encoder.encode("password123"))
                        .roles("USER")
                        .build(),
                User.builder()
                        .username("admin")
                        .password(encoder.encode("admin-s3cr3t!"))
                        .roles("ADMIN")
                        .build()
        );
    }
}
