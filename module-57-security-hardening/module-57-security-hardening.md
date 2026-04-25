---
title: "Module 57 — Security Hardening"
nav_order: 57
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-57-security-hardening/src){: .btn .btn-outline }

# Module 57 — Security Hardening

## What this module covers

Spring Security configuration for a REST API, OWASP Top 10 defences in practice,
OWASP Dependency Check for CVE scanning, and HashiCorp Vault for secrets management.
Tests verify access-control policy using `@WithMockUser` and security headers using
Spring MockMvc.

---

## Project structure

```
src/main/java/com/javatraining/security/
├── SecurityHardeningApplication.java
├── config/
│   └── SecurityConfig.java      # Spring Security: auth, authz, headers, BCrypt
└── api/
    ├── RegisterRequest.java     # record with @NotBlank, @Size validation
    ├── PublicController.java    # GET /api/public/info  — no auth
    ├── AuthController.java      # POST /api/auth/register — no auth, @Valid
    ├── UserController.java      # GET /api/user/me — USER or ADMIN
    └── AdminController.java     # DELETE /api/admin/users/{id} — ADMIN only

src/test/java/com/javatraining/security/
├── AccessControlTest.java       # @WithMockUser: 401, 403, 200 (3 tests)
└── SecurityFeaturesTest.java    # security headers + input validation (2 tests)
```

---

## OWASP Top 10 coverage

| # | Vulnerability | Defence in this module |
|---|---|---|
| A01 | Broken Access Control | `hasRole("ADMIN")` on admin paths; 401 for unauthenticated, 403 for wrong role |
| A02 | Cryptographic Failures | `BCryptPasswordEncoder(12)` — ~300ms per hash, rainbow tables impractical |
| A03 | Injection | Bean Validation on `RegisterRequest`; Spring Data JPA uses parameterized queries |
| A05 | Security Misconfiguration | `headers(Customizer.withDefaults())` — DENY framing, nosniff, cache control |
| A07 | Identification & Auth Failures | Spring Security rejects unauthenticated requests before reaching controllers |
| A09 | Security Logging & Monitoring | Spring Security logs auth failures; add audit log in `AuthenticationEventPublisher` |

---

## Spring Security configuration

```java
@Bean
SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()           // all other paths need a valid user
        )
        .httpBasic(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)      // stateless API — no session cookies
        .headers(Customizer.withDefaults())         // all security headers enabled
        .build();
}
```

### CSRF note

CSRF attacks exploit session cookies. Stateless APIs authenticating with tokens
(Bearer JWT, Basic Auth header) are not vulnerable to CSRF — disabling CSRF protection
is safe and removes the overhead of CSRF token synchronisation. Session-based apps
(Thymeleaf, MVC form submissions) **must** keep CSRF enabled.

### Security headers set by `headers(Customizer.withDefaults())`

| Header | Value | Protects against |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | MIME-type confusion attacks |
| `X-Frame-Options` | `DENY` | Clickjacking (iframes) |
| `X-XSS-Protection` | `0` | Disabled — modern browsers use CSP; this header is obsolete |
| `Cache-Control` | `no-store` | Sensitive data cached in browser history |
| `Strict-Transport-Security` | `max-age=31536000` | HTTP downgrade (added for HTTPS requests only) |

---

## Password hashing — BCrypt

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // work factor 12 ≈ 300ms per hash
}
```

BCrypt is a **one-way adaptive hash** — the work factor (cost) increases hashing time,
and can be raised as hardware speeds up without invalidating existing hashes
(the factor is stored in the hash string).

### What not to use

| Algorithm | Problem |
|---|---|
| MD5, SHA-1 | Not designed for passwords — microseconds to hash, rainbow tables exist |
| SHA-256 (unsalted) | Same: fast, no salt by default |
| Plain text | Obviously never |

`PasswordEncoderFactories.createDelegatingPasswordEncoder()` returns a delegating encoder
that stores the algorithm ID in the hash (`{bcrypt}$2a$12$...`), enabling migration to
stronger algorithms without forcing a password reset.

---

## Input validation — OWASP A03 (Injection prevention)

```java
public record RegisterRequest(
    @NotBlank(message = "Username must not be blank")
    String username,

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}

@PostMapping("/register")
@ResponseStatus(HttpStatus.CREATED)
public Map<String, String> register(@Valid @RequestBody RegisterRequest request) { ... }
```

`@Valid` triggers `MethodArgumentNotValidException` before the method body runs.
Spring Boot's default error handler maps this to **HTTP 400 Bad Request**.

Beyond this, JPA's parameterized queries prevent SQL injection — user input is always
bound as a parameter, never concatenated into SQL:
```java
userRepository.findByUsername(username);     // safe: Spring Data generates a prepared statement
// Never: entityManager.createQuery("... WHERE u.name = '" + username + "'");
```

---

## Testing with `@WithMockUser`

```java
@SpringBootTest
@AutoConfigureMockMvc
class AccessControlTest {

    @Test
    void unauthenticated_request_to_protected_endpoint_returns_401() throws Exception {
        mockMvc.perform(get("/api/user/me"))
                .andExpect(status().isUnauthorized());
    }

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
```

`@WithMockUser` injects a synthetic `SecurityContext` directly — no HTTP credentials
are submitted, the `UserDetailsService` is not called. This isolates the **access-control
policy** under test from the **authentication mechanism**.

`roles = "USER"` → authority `ROLE_USER`. `hasRole("ADMIN")` → checks `ROLE_ADMIN`. No match → 403.

---

## OWASP Dependency Check

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.4</version>
    <configuration>
        <!-- Fail the build if any dependency has a CVSS score >= 7 (High) -->
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
    </configuration>
</plugin>
```

Run in CI (not bound to `mvn test` — it downloads the NVD database on first run):
```bash
mvn dependency-check:check
```

Output: `target/dependency-check-report.html` with each dependency's CVE list.

### Suppression file

When a CVE is a false positive or has been mitigated:
```xml
<!-- dependency-check-suppressions.xml -->
<suppressions>
    <suppress>
        <notes>CVE-2023-XXXXX: not exploitable — we don't use the affected feature</notes>
        <cve>CVE-2023-XXXXX</cve>
    </suppress>
</suppressions>
```

### CI integration

Add to `ci.yml` as a separate step that runs after `mvn verify`:
```yaml
- name: Dependency vulnerability scan
  run: mvn dependency-check:check
  continue-on-error: false   # block merges on unacknowledged High/Critical CVEs
```

---

## HashiCorp Vault

Vault provides centralised secrets management: database passwords, API keys, certificates.
Spring Cloud Vault integrates Vault as a Spring `PropertySource`.

### Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

### Configuration

```yaml
# application.yml
spring:
  cloud:
    vault:
      host: vault
      port: 8200
      token: ${VAULT_TOKEN}           # never hardcode
      kv:
        enabled: true
        backend: secret
        default-context: security-demo  # reads secret/security-demo
```

Vault path `secret/security-demo` contains:
```bash
vault kv put secret/security-demo \
  db.password=s3cr3t-db-pw \
  jwt.secret=super-secret-key
```

Spring Cloud Vault makes these available as Spring properties:
```java
@Value("${db.password}")
private String dbPassword;
```

### Testing without a running Vault

Use `@TestPropertySource` to supply mock values:
```java
@SpringBootTest
@TestPropertySource(properties = {
    "db.password=test-password",
    "jwt.secret=test-secret-key"
})
class MyServiceTest { ... }
```

Or, disable Vault for the test profile in `application-test.properties`:
```properties
spring.cloud.vault.enabled=false
```

### Dynamic secrets (advanced)

Vault can generate short-lived, auto-rotating database credentials:
```bash
vault secrets enable database
vault write database/roles/myapp \
  db_name=mydb \
  creation_statements="CREATE USER '{{name}}'@'%' ..." \
  default_ttl=1h max_ttl=24h
```

The app requests a new credential on startup and Vault revokes it automatically after `default_ttl`.
Compromised credentials expire without manual rotation.

---

## Tests

| Class | OWASP | Tests |
|---|---|---|
| `AccessControlTest` | A01 | 3 |
| `SecurityFeaturesTest` | A03, A05 | 2 |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **5/5 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `@WithMockUser` over `httpBasic()` + real credentials in tests | Isolates the authorization policy from the authentication mechanism; no need to store real test passwords |
| `BCryptPasswordEncoder(12)` over default strength (10) | Strength 12 ≈ 300ms; raises the cost of brute force from the default 100ms without being noticeable to users |
| `csrf(AbstractHttpConfigurer::disable)` | Stateless API — CSRF requires browser-managed session cookies, which this API doesn't use |
| `headers(Customizer.withDefaults())` | Enables all Spring Security headers in one call; explicit `withDefaults()` makes intent clear and prevents accidental omission |
| OWASP Dependency Check NOT bound to `mvn test` | First run downloads the NVD CVE database (~few hundred MB); binding to test would make every local build slow — run in CI explicitly |
