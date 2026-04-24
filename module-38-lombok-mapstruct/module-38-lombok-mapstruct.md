# Module 38 — Lombok & MapStruct

Two annotation-processing tools that eliminate boilerplate without runtime cost:
**Lombok** — generates getters, setters, constructors, builders, and loggers at compile time;
**MapStruct** — generates type-safe DTO ↔ entity mapping code at compile time.

Both tools produce plain Java source files in `target/generated-sources/` — no reflection, no proxies, no startup overhead.

---

## Annotation Processor Setup

```xml
<!-- pom.xml: Lombok MUST be listed before mapstruct-processor -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.5.5.Final</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

```
  Why order matters:
  MapStruct calls Lombok-generated getters and setters.
  If MapStruct runs first, those methods don't exist yet → compilation error.
  Lombok must generate its accessors before MapStruct reads them.
```

---

## Lombok Annotations

### @Data

```java
@Data   // shorthand for: @Getter + @Setter + @ToString +
        //                @EqualsAndHashCode + @RequiredArgsConstructor
public class Address {
    private String street;
    private String city;
    private String country;
}

// Generated:
//   getStreet(), setStreet(), ... for every field
//   toString() → "Address(street=..., city=..., country=...)"
//   equals() + hashCode() using ALL fields
//   Address() — no-args constructor (no final/required fields → empty constructor)
```

### @Builder

```java
@Builder
public class User {
    private Long id;
    private String firstName;
    private String email;
}

// Usage:
User user = User.builder()
        .id(1L)
        .firstName("Alice")
        .email("alice@example.com")
        .build();

// Generated: UserBuilder inner class with method chaining + build()
```

### @Builder.Default — Required for Field Initializers

```java
// PITFALL: without @Builder.Default, field initializers are IGNORED by the builder
@Builder
public class Task {
    private List<String> tags = new ArrayList<>();  // WARNING: builder ignores this
}
Task.builder().build().getTags();  // → null (not empty list!)

// FIX: @Builder.Default preserves the initializer
@Builder
public class Task {
    @Builder.Default
    private List<String> tags = new ArrayList<>();  // builder uses this
}
Task.builder().build().getTags();  // → [] (empty list, as expected)
```

### @Builder + @NoArgsConstructor + @AllArgsConstructor

```java
// PITFALL: @Builder generates a package-private all-args constructor,
// removing Java's implicit no-args constructor.
// → Jackson deserialization fails, JPA proxy creation fails, new Foo() fails.

// FIX: declare both explicitly
@Data
@Builder
@NoArgsConstructor   // restores no-args constructor
@AllArgsConstructor  // required by @Builder — provides the all-args constructor
public class CreateUserRequest {
    private String firstName;
    private String lastName;
    private String email;
}

// Now both work:
new CreateUserRequest()                            // Jackson/JPA
CreateUserRequest.builder().email("x").build()    // fluent construction
```

### @Value — Immutable Value Object

```java
@Value  // all fields: private final, no setters, all-args constructor, equals, hashCode, toString
@Builder
public class Product {
    Long id;
    String name;
    BigDecimal price;
    String category;
}

// setName() does not exist — compile error if called
// equals/hashCode: based on all fields (structural equality)
Product p1 = Product.builder().id(1L).name("Laptop").price(new BigDecimal("999")).build();
Product p2 = Product.builder().id(1L).name("Laptop").price(new BigDecimal("999")).build();
p1.equals(p2);  // → true (same data)
```

### @Slf4j + @RequiredArgsConstructor

```java
@Service
@Slf4j                    // generates: private static final Logger log = LoggerFactory.getLogger(...)
@RequiredArgsConstructor  // generates constructor for every final or @NonNull field
public class NotificationService {

    private final EmailGateway emailGateway;  // FINAL → in @RequiredArgsConstructor constructor
    private int sentCount = 0;                // non-final → NOT in constructor

    public String send(String recipient, String message) {
        log.info("Sending to {}: {}", recipient, message);  // log is available immediately
        sentCount++;
        return emailGateway.deliver(recipient, message);
    }
}

// Spring uses the single constructor (no @Autowired needed):
// new NotificationService(emailGateway)
```

### Fine-Grained vs @Data for Entities

```java
// Prefer fine-grained annotations on entities:
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "address")             // avoid verbose/circular output
@EqualsAndHashCode(of = {"id", "email"})   // only stable identity fields
public class User {
    private Long id;
    private String firstName;
    private String email;
    private Address address;
}

// Avoid @Data on JPA entities because:
//   @Data's @EqualsAndHashCode includes ALL fields
//   → lazy collection access in JPA sessions
//   → two unsaved entities (id = null) are equal even if they represent different rows
//   → toString traverses all relationships → LazyInitializationException outside session
```

---

## Lombok Pitfalls

### 1. Mutable EqualsAndHashCode Breaks Collections

```
  Object added to HashSet → stored in bucket based on hashCode() at that moment.
  If any field used in hashCode() is mutated → new hash → different bucket.
  HashSet.contains(object) → looks in WRONG bucket → returns false.
  The object is "lost" — still in the set but unreachable.

  Fix: @EqualsAndHashCode(of = "id") — use only the stable identity field.
  For JPA entities: use only @Id in equals/hashCode.
```

```java
set.contains(key)  // before mutation → true
key.setEmail("new@example.com");
set.contains(key)  // after mutation  → false! (bucket lookup fails)
set.size()         // → 1 (object still there, just unreachable)
```

### 2. @Builder.Default Required for Initializers

```
  @Builder ignores field initializers unless @Builder.Default is present.
  builder().build().getTags() → null (not the expected empty list).
```

### 3. @Builder Removes No-Args Constructor

```
  @Builder alone: no new Foo() possible → Jackson fails, JPA fails.
  Fix: always pair with @NoArgsConstructor + @AllArgsConstructor.
```

### 4. @ToString Circular Reference

```
  A → @ToString includes B → B @ToString includes A → StackOverflowError.
  Fix: @ToString(exclude = "backReference") on the owning side.
```

---

## MapStruct

### Why MapStruct Instead of Manual Mapping?

```
  Manual mapping              MapStruct
  ─────────────────────────   ────────────────────────────────────────────
  user.getFirstName() +       @Mapping(expression = "java(...)")
    " " + user.getLastName()  → generated once, used everywhere
  Written per-method          Interface with method signatures only
  No null checks              MapStruct generates null-safe nested paths
  Runtime reflection (Dozer)  Compile-time code generation (no reflection)
  Easy to miss a field        Unmapped fields produce warnings by default
```

### @Mapper Setup

```java
// componentModel = "spring": MapStruct generates @Component on the impl class
// Spring auto-discovers it → inject via @Autowired or constructor
@Mapper(componentModel = "spring")
public interface UserMapper {
    // methods...
}

// Generated: UserMapperImpl.java in target/generated-sources/annotations/
// Inspect it — it's plain Java code, fully readable
```

### Mapping Patterns

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    // 1. Ignore a field (never mapped from source)
    @Mapping(target = "id", ignore = true)
    User requestToUser(CreateUserRequest request);

    // 2. Expression: combine two fields into one
    @Mapping(target = "fullName",
             expression = "java(user.getFirstName() + \" \" + user.getLastName())")
    UserDto userToDto(User user);

    // 3. Nested source: flatten address.city → city
    //    MapStruct generates a null-safe path:
    //    if (user.getAddress() != null) dto.setCity(user.getAddress().getCity());
    @Mapping(target = "city", source = "address.city")
    UserDto userToDto(User user);

    // 4. List mapping: auto-generated from the single-item method
    List<UserDto> usersToDtos(List<User> users);

    // 5. Partial update (PATCH): null source fields don't overwrite target
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromRequest(CreateUserRequest request, @MappingTarget User user);
}
```

### Partial Update — PATCH Semantics

```
  Traditional PUT: send all fields, overwrite everything.
  PATCH: send only changed fields; null means "leave unchanged".

  @BeanMapping(nullValuePropertyMappingStrategy = IGNORE) + @MappingTarget:
    source.email = "new@example.com" → user.email = "new@example.com" (updated)
    source.firstName = null          → user.firstName unchanged             (skipped)
    source.role = null               → user.role unchanged                  (skipped)

  Use case: REST PATCH endpoint where clients send partial updates.
```

### Inspecting Generated Code

```
  After mvn compile, open:
  target/generated-sources/annotations/com/example/UserMapperImpl.java

  You'll see:
    @Component   ← because componentModel = "spring"
    public class UserMapperImpl implements UserMapper {
        @Override
        public UserDto userToDto(User user) {
            if (user == null) { return null; }      ← null-safe
            String city = null;
            if (user.getAddress() != null) {         ← null-safe nested path
                city = user.getAddress().getCity();
            }
            ...
        }
    }
```

---

## Module 38 — What Was Built

```
  module-38-lombok-mapstruct/
  ├── pom.xml     (Spring Boot 3.3.5, lombok, mapstruct 1.5.5.Final,
  │               compiler plugin with ordered annotationProcessorPaths)
  └── src/
      ├── main/java/com/javatraining/lombokstruct/
      │   ├── LombokStructApplication.java
      │   ├── entity/
      │   │   ├── User.java        — fine-grained: @Getter @Setter @Builder @ToString(exclude)
      │   │   │                      @EqualsAndHashCode(of=...) @NoArgsConstructor @AllArgsConstructor
      │   │   ├── Address.java     — @Data @Builder @NoArgsConstructor @AllArgsConstructor
      │   │   └── Product.java     — @Value @Builder (immutable)
      │   ├── service/
      │   │   ├── NotificationService.java  — @Slf4j @RequiredArgsConstructor
      │   │   └── EmailGateway.java         — @Slf4j @Component
      │   ├── dto/
      │   │   ├── UserDto.java              — @Data @Builder (fullName, city flattened)
      │   │   └── CreateUserRequest.java    — @Data @Builder @NoArgsConstructor @AllArgsConstructor
      │   └── mapper/
      │       └── UserMapper.java           — @Mapper(componentModel="spring"),
      │                                       ignore, expression, nested source, list, partial update
      └── test/java/com/javatraining/lombokstruct/
          ├── LombokAnnotationsTest.java  9 tests — @Builder, @Data, @Value, @Slf4j, @RequiredArgsConstructor
          ├── LombokPitfallsTest.java     5 tests — mutable HashSet, @Builder.Default, no-args constructor,
          │                                         circular @ToString
          └── MapStructMappingTest.java   9 tests — requestToUser (ignore fields), userToDto (expression,
                                                    nested source, null-safe), list mapping, partial update
```

All tests: **23 passing** (requires Java 21; MapStruct processor is incompatible with Java 25+).

---

## Key Takeaways

```
  Processor order  Lombok before mapstruct-processor — MapStruct calls Lombok getters

  @Data            All-in-one for simple POJOs; avoid on JPA entities
  @Builder         Fluent construction; requires @NoArgsConstructor + @AllArgsConstructor
                   if a no-args constructor is also needed (Jackson, JPA)
  @Builder.Default Required for field initializers — without it, builder sets null
  @Value           Immutable value object: all final, no setters, structural equals
  @Slf4j           Injects private static final Logger log = LoggerFactory.getLogger(...)
  @RequiredArgsConstructor  Constructor for final/@NonNull fields; non-final fields excluded
  @EqualsAndHashCode(of = "id")  Use only stable fields; mutable fields break HashSets

  MapStruct        Compile-time mapping: no reflection, no runtime cost
  ignore = true    Field never mapped from source (e.g. server-assigned id)
  expression       Java code inline in the annotation; combine fields, call methods
  source = "a.b"   Null-safe nested path: MapStruct generates null checks
  List<DTO>        Auto-generated from single-item mapper method
  IGNORE strategy  Null source fields skip the target field — PATCH semantics
  @MappingTarget   Write into existing object instead of creating a new one
```
