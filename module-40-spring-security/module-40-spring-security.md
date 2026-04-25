---
title: "Module 40 — Spring Security"
parent: "Phase 5 — Spring Ecosystem"
nav_order: 40
render_with_liquid: false
---
{% raw %}

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-40-spring-security/src){: .btn .btn-outline }

# Module 40 — Spring Security

Securing a Spring Boot REST API layer by layer:
**SecurityFilterChain** for HTTP-level access control,
**JWT** for stateless authentication (no server-side sessions),
**BCrypt** for password hashing,
**@PreAuthorize** for method-level role enforcement,
and **Spring Security Test** for `@WithMockUser` slice tests.

---

## SecurityFilterChain

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // activates @PreAuthorize, @PostAuthorize, @Secured
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            // CSRF protection is session-based; stateless JWT APIs don't use cookies
            .csrf(AbstractHttpConfigurer::disable)

            // No HttpSession — each request must prove identity via JWT
            .sessionManagement(sm ->
                    sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules — evaluated top-to-bottom, first match wins
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products").permitAll()
                    .anyRequest().authenticated()
            )

            // Disable HTTP Basic: prevents "WWW-Authenticate: Basic" on 401 responses.
            // Without this, Apache HttpClient retries POST requests on 401 (non-repeatable body → error).
            .httpBasic(AbstractHttpConfigurer::disable)

            // Return plain 401 for unauthenticated requests (no redirect to /login)
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // JWT filter runs before Spring's own UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

```
  Authorization rule evaluation:
    /api/auth/**           → permitAll  — login endpoint, no token required
    GET /api/products      → permitAll  — public product list
    any other request      → authenticated  — must have valid JWT

  First match wins: more specific rules must come before broader ones.
```

---

## In-Memory Users + BCrypt

```java
@Bean
public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    UserDetails user = User.withUsername("user")
            .password(encoder.encode("password"))
            .roles("USER")      // stored as ROLE_USER in GrantedAuthority
            .build();
    UserDetails admin = User.withUsername("admin")
            .password(encoder.encode("admin123"))
            .roles("ADMIN")     // stored as ROLE_ADMIN
            .build();
    return new InMemoryUserDetailsManager(user, admin);
}

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
    // BCrypt: adaptive one-way hash — deliberately slow to resist brute-force.
    // Cost factor (default 10) means ~100ms per hash — fine for login, brutal for attackers.
    // Never store plain or MD5/SHA-1 passwords.
}
```

---

## JWT — Stateless Authentication

### Why Stateless?

```
  Session-based              JWT (stateless)
  ─────────────────────────  ──────────────────────────────────────────────────
  Server stores session data  No server state; token is self-contained
  Single server or sticky     Works across multiple servers / pods
  Session invalidation easy   Token revocation requires extra infrastructure
  Cookie-based CSRF risk      Bearer token in Authorization header — CSRF-safe
```

### JwtUtil

```java
@Component
public class JwtUtil {

    private final String secret;     // from application.properties — at least 32 bytes
    private final long expirationMs;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    // Signed JWT: header.payload.signature (base64url encoded, dot-separated)
    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())      // HMAC-SHA-256
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        Date expiration = parseClaims(token).getExpiration();
        return username.equals(userDetails.getUsername()) && expiration.after(new Date());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // HMAC-SHA-256 requires at least 256 bits (32 bytes).
        // Keys.hmacShaKeyFor() throws WeakKeyException if the key is too short.
    }
}
```

### JwtAuthenticationFilter

```java
// OncePerRequestFilter: guaranteed to run exactly once per HTTP request.
// Placed before UsernamePasswordAuthenticationFilter in the chain.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);  // strip "Bearer "
            try {
                String username = jwtUtil.extractUsername(token);
                // Don't overwrite an auth that's already in the context (e.g., from a test)
                if (username != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.isTokenValid(token, userDetails)) {
                        // Build authentication and populate SecurityContext
                        UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception ignored) {
                // Invalid/expired/malformed token — no auth set → downstream returns 401
            }
        }

        chain.doFilter(request, response);  // always continue the chain
    }
}
```

### AuthController — Login Endpoint

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // DaoAuthenticationProvider verifies credentials against UserDetailsService.
        // Throws BadCredentialsException if username not found or password wrong.
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()));

        String token = jwtUtil.generateToken((UserDetails) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}

// GlobalExceptionHandler maps BadCredentialsException → 401 ProblemDetail:
// { "title": "Authentication Failed", "status": 401, "detail": "Invalid username or password" }
```

---

## @PreAuthorize — Method-Level Security

```java
// @EnableMethodSecurity on SecurityConfig activates method security.
// @PreAuthorize is checked AFTER authentication passes the filter chain.
// A ROLE_USER authenticated user hitting an @PreAuthorize("hasRole('ADMIN')") method → 403.

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping         // no @PreAuthorize — public (permitAll in filter chain)
    public ResponseEntity<List<Product>> getAll() { ... }

    @GetMapping("/{id}") // no @PreAuthorize — protected by filter chain (anyRequest().authenticated())
    public ResponseEntity<Product> getById(@PathVariable Long id) { ... }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")   // → 403 for ROLE_USER; → proceed for ROLE_ADMIN
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) { ... }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) { ... }
}
```

```
  hasRole('ADMIN')        — true if SecurityContext has ROLE_ADMIN GrantedAuthority
  hasAnyRole('A','B')     — true if either role is present
  isAuthenticated()       — true if user is authenticated (not anonymous)
  #id == principal.id     — SpEL: compare method arg to authenticated user's ID
  @beanName.method(#arg)  — delegate to a Spring bean for complex logic
```

---

## Spring Security Test

### @WithMockUser — Bypassing JWT in Tests

```java
// @WithMockUser injects a synthetic UsernamePasswordAuthenticationToken into the
// SecurityContext before the test runs. JwtAuthenticationFilter checks:
//   if (SecurityContextHolder.getContext().getAuthentication() == null) { ... }
// Since the context is already populated, the JWT filter skips JWT processing entirely.

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthorizationTest {

    @Autowired MockMvc mockMvc;
    @MockBean ProductService productService;

    @Test
    void anonymous_user_cannot_get_product_by_id() throws Exception {
        // No auth, no JWT → 401
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_role_cannot_create_product() throws Exception {
        // @PreAuthorize("hasRole('ADMIN')") — USER has no ADMIN role → 403
        mockMvc.perform(post("/api/products")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":1,\"category\":\"c\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_role_can_create_product() throws Exception {
        given(productService.create(any())).willReturn(new Product(1L, "x", TEN, "c"));
        mockMvc.perform(post("/api/products")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":1,\"category\":\"c\"}"))
                .andExpect(status().isCreated());
    }
}
```

### JWT Integration Test with MockMvc

```java
// Full end-to-end test: login → extract token → use token in subsequent requests.
// Using MockMvc (not TestRestTemplate) avoids Apache HttpClient's automatic
// auth-retry on 401 responses (NonRepeatableRequestException on POST bodies).

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class JwtFlowTest {

    private String loginAndGetToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void user_jwt_cannot_create_product_returns_403() throws Exception {
        String userToken = loginAndGetToken("user", "password");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Laptop\",\"price\":999,\"category\":\"Electronics\"}"))
                .andExpect(status().isForbidden());  // ROLE_USER → @PreAuthorize fails
    }

    @Test
    void admin_jwt_creates_product_and_user_jwt_reads_it() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        // Admin creates
        String body = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Keyboard\",\"price\":149,\"category\":\"Accessories\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        // User reads (only needs ROLE_USER which any authenticated user has)
        String userToken = loginAndGetToken("user", "password");
        mockMvc.perform(get("/api/products/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }
}
```

---

## Module 40 — What Was Built

```
  module-40-spring-security/
  ├── pom.xml     (Spring Boot 3.3.5, spring-boot-starter-security,
  │               jjwt-api/impl/jackson 0.12.6, lombok, spring-security-test)
  └── src/
      ├── main/java/com/javatraining/springsecurity/
      │   ├── SpringSecurityApplication.java
      │   ├── model/Product.java            — @Data @Builder
      │   ├── dto/LoginRequest.java         — record(username, password)
      │   ├── dto/LoginResponse.java        — record(token)
      │   ├── dto/ProductRequest.java       — record + @NotBlank @DecimalMin
      │   ├── exception/
      │   │   ├── ProductNotFoundException.java
      │   │   └── GlobalExceptionHandler.java  — BadCredentialsException → 401,
      │   │                                      ProductNotFoundException → 404
      │   ├── security/
      │   │   ├── JwtUtil.java              — generate/validate JWT, @Value injection
      │   │   └── JwtAuthenticationFilter.java — OncePerRequestFilter, Bearer token
      │   ├── config/
      │   │   └── SecurityConfig.java       — SecurityFilterChain, UserDetailsService,
      │   │                                   BCryptPasswordEncoder, @EnableMethodSecurity
      │   ├── service/ProductService.java   — ConcurrentHashMap in-memory store
      │   └── controller/
      │       ├── ProductController.java    — @PreAuthorize on POST and DELETE
      │       └── AuthController.java       — POST /api/auth/login → JWT
      ├── main/resources/application.properties
      └── test/java/com/javatraining/springsecurity/
          ├── AuthorizationTest.java   — 7 tests: @SpringBootTest(MOCK) + @AutoConfigureMockMvc
          │                              @WithMockUser, anonymous 401, user 403, admin OK
          └── JwtFlowTest.java         — 7 tests: @SpringBootTest(MOCK) + @AutoConfigureMockMvc
                                         valid login (token), wrong password (401),
                                         no token (401), invalid token (401),
                                         valid user JWT (accesses protected endpoint),
                                         user JWT cannot POST (403),
                                         admin JWT creates + user JWT reads
```

All tests: **14 passing**.

---

## Key Takeaways

```
  SecurityFilterChain     Replaces the auto-configured default; define access rules top-to-bottom
  permitAll()             No authentication required — public endpoints
  anyRequest().authenticated()  All other endpoints need a valid principal
  SessionCreationPolicy.STATELESS  No HttpSession — each request carries its own token
  csrf(disable)           CSRF attacks rely on cookies; Bearer tokens are CSRF-safe
  httpBasic(disable)      Removes "WWW-Authenticate: Basic" from 401 responses;
                          prevents HTTP client auth-retry on POST requests
  HttpStatusEntryPoint    Returns plain 401 for unauthenticated access (no redirect)

  BCryptPasswordEncoder   Adaptive hash — deliberately slow; always encode passwords
  InMemoryUserDetailsManager  Development/test only; production uses JdbcUserDetailsManager
                              or a custom UserDetailsService backed by a database

  JwtUtil                 Stateless token utility: generate (sign) + validate (verify + expiry)
  Keys.hmacShaKeyFor()    Rejects keys shorter than 256 bits — enforced at construction time
  OncePerRequestFilter    Guaranteed single execution per request; base class for JWT filter
  SecurityContextHolder   Thread-local holder; set authentication here to mark request as authed

  @EnableMethodSecurity   Required for @PreAuthorize to work; add to SecurityConfig
  @PreAuthorize           SpEL checked before method runs; 403 on failure
  hasRole('ADMIN')        Checks for ROLE_ADMIN in GrantedAuthority (Spring prepends "ROLE_")

  @WithMockUser           Test annotation: injects synthetic auth into SecurityContext;
                          JWT filter skips processing when context is already populated
  @AutoConfigureMockMvc   Combines with @SpringBootTest to provide MockMvc without real server
  MockMvc vs TestRestTemplate  Use MockMvc for security tests — no HTTP client auth-retry issues
```
{% endraw %}
