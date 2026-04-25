package com.javatraining.encapsulation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * TOPIC: Full design - integrating every encapsulation concept
 *
 * UserProfile is:
 *   • Immutable core (all private final, no setters)
 *   • Built via Step Builder (required fields enforced at compile time)
 *   • Validated in the builder's build() and in compact record constructors
 *   • Defensive copies on mutable fields (roles list, avatar bytes)
 *   • "withX" copy helpers for non-destructive updates
 *   • equals/hashCode on natural key (userId)
 */
public class UserProfile {

    // -------------------------------------------------------------------------
    // Nested value objects (records are immutable by default)
    // -------------------------------------------------------------------------
    record Address(String street, String city, String state, String pincode) {
        private static final Pattern PIN = Pattern.compile("\\d{6}");

        Address {
            Objects.requireNonNull(street,  "street");
            Objects.requireNonNull(city,    "city");
            Objects.requireNonNull(state,   "state");
            if (pincode == null || !PIN.matcher(pincode).matches())
                throw new IllegalArgumentException("pincode must be 6 digits: " + pincode);
        }

        @Override public String toString() {
            return street + ", " + city + ", " + state + " - " + pincode;
        }
    }

    enum AccountStatus { PENDING, ACTIVE, SUSPENDED, DELETED }

    // -------------------------------------------------------------------------
    // Main immutable class
    // -------------------------------------------------------------------------
    private static final Pattern EMAIL_RE =
        Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final String        userId;         // required, immutable identity
    private final String        username;       // required
    private final String        email;          // required, validated
    private final String        displayName;    // required
    private final LocalDate     dateOfBirth;    // required
    private final Address       address;        // optional
    private final List<String>  roles;          // optional, defensive copy
    private final AccountStatus status;
    private final boolean       emailVerified;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String        bio;            // optional
    private final byte[]        avatarBytes;    // optional, mutable - must copy

    private UserProfile(Builder b) {
        this.userId        = b.userId;
        this.username      = b.username;
        this.email         = b.email;
        this.displayName   = b.displayName;
        this.dateOfBirth   = b.dateOfBirth;
        this.address       = b.address;
        // Defensive copy of list IN
        this.roles         = Collections.unmodifiableList(
                                new ArrayList<>(b.roles));
        this.status        = b.status;
        this.emailVerified = b.emailVerified;
        this.createdAt     = b.createdAt;
        this.updatedAt     = b.updatedAt;
        this.bio           = b.bio;
        // Defensive copy of byte array IN
        this.avatarBytes   = b.avatarBytes != null
                                ? b.avatarBytes.clone()
                                : null;
    }

    // ── Accessors ────────────────────────────────────────────────────────────
    public String        userId()        { return userId; }
    public String        username()      { return username; }
    public String        email()         { return email; }
    public String        displayName()   { return displayName; }
    public LocalDate     dateOfBirth()   { return dateOfBirth; }
    public Address       address()       { return address; }
    public AccountStatus status()        { return status; }
    public boolean       emailVerified() { return emailVerified; }
    public LocalDateTime createdAt()     { return createdAt; }
    public LocalDateTime updatedAt()     { return updatedAt; }
    public String        bio()           { return bio; }
    public boolean       hasAvatar()     { return avatarBytes != null; }

    // Defensive copy OUT for mutable types
    public List<String> roles() { return new ArrayList<>(roles); }
    public byte[]       avatarBytes() {
        return avatarBytes != null ? avatarBytes.clone() : null;
    }

    public boolean hasRole(String role) { return roles.contains(role); }
    public boolean isActive()           { return status == AccountStatus.ACTIVE; }

    // ── "with" copy helpers - toBuilder() returns the concrete Builder class
    //    so we can call any setter freely, bypassing the step-order constraint.
    public UserProfile withEmail(String newEmail) {
        Builder b = toBuilder(); b.email = Builder.validEmail(newEmail);
        b.updatedNow(); return b.build();
    }

    public UserProfile withAddress(Address newAddress) {
        Builder b = toBuilder(); b.address = newAddress;
        b.updatedNow(); return b.build();
    }

    public UserProfile withStatus(AccountStatus newStatus) {
        Builder b = toBuilder(); b.status = newStatus;
        b.updatedNow(); return b.build();
    }

    public UserProfile withRole(String role) {
        List<String> updated = new ArrayList<>(roles);
        if (!updated.contains(role)) updated.add(role);
        Builder b = toBuilder(); b.roles = updated;
        b.updatedNow(); return b.build();
    }

    public UserProfile withoutRole(String role) {
        List<String> updated = new ArrayList<>(roles);
        updated.remove(role);
        Builder b = toBuilder(); b.roles = updated;
        b.updatedNow(); return b.build();
    }

    public UserProfile verified() {
        Builder b = toBuilder(); b.emailVerified = true;
        b.updatedNow(); return b.build();
    }

    // ── equals / hashCode on natural key ─────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UserProfile u)) return false;
        return userId.equals(u.userId);
    }

    @Override public int hashCode() { return userId.hashCode(); }

    @Override
    public String toString() {
        return String.format(
            "UserProfile{userId=%s, username=%s, email=%s, status=%s, roles=%s, verified=%s}",
            userId, username, email, status, roles, emailVerified);
    }

    // ── Step Builder ──────────────────────────────────────────────────────────
    public interface UserIdStep    { UsernameStep    userId(String id); }
    public interface UsernameStep  { EmailStep       username(String username); }
    public interface EmailStep     { DisplayNameStep email(String email); }
    public interface DisplayNameStep { DobStep       displayName(String name); }
    public interface DobStep       { BuildStep       dateOfBirth(LocalDate dob); }

    public interface BuildStep {
        BuildStep      address(Address address);
        BuildStep      roles(List<String> roles);
        BuildStep      role(String role);
        BuildStep      status(AccountStatus status);
        BuildStep      emailVerified(boolean verified);
        BuildStep      bio(String bio);
        BuildStep      avatarBytes(byte[] bytes);
        BuildStep      updatedNow();
        UserProfile    build();
    }

    public static UserIdStep builder() { return new Builder(); }
    public Builder toBuilder()         { return new Builder(this); }

    public static final class Builder
            implements UserIdStep, UsernameStep, EmailStep, DisplayNameStep, DobStep, BuildStep {

        private String        userId;
        private String        username;
        private String        email;
        private String        displayName;
        private LocalDate     dateOfBirth;
        private Address       address      = null;
        private List<String>  roles        = new ArrayList<>();
        private AccountStatus status       = AccountStatus.PENDING;
        private boolean       emailVerified = false;
        private LocalDateTime createdAt    = LocalDateTime.now();
        private LocalDateTime updatedAt    = LocalDateTime.now();
        private String        bio          = null;
        private byte[]        avatarBytes  = null;

        private Builder() {}

        private Builder(UserProfile p) {
            this.userId        = p.userId;
            this.username      = p.username;
            this.email         = p.email;
            this.displayName   = p.displayName;
            this.dateOfBirth   = p.dateOfBirth;
            this.address       = p.address;
            this.roles         = new ArrayList<>(p.roles);
            this.status        = p.status;
            this.emailVerified = p.emailVerified;
            this.createdAt     = p.createdAt;
            this.updatedAt     = p.updatedAt;
            this.bio           = p.bio;
            this.avatarBytes   = p.avatarBytes != null ? p.avatarBytes.clone() : null;
        }

        @Override public UsernameStep    userId(String id)     { userId      = req(id,  "userId");      return this; }
        @Override public EmailStep       username(String u)    { username    = req(u,   "username");    return this; }
        @Override public DisplayNameStep email(String e)       { email       = validEmail(e);           return this; }
        @Override public DobStep         displayName(String n) { displayName = req(n,   "displayName"); return this; }
        @Override public BuildStep       dateOfBirth(LocalDate d){
            if (d == null || d.isAfter(LocalDate.now()))
                throw new IllegalArgumentException("Invalid date of birth");
            dateOfBirth = d; return this;
        }
        @Override public BuildStep address(Address a)          { address       = a;             return this; }
        @Override public BuildStep roles(List<String> r)       { roles         = new ArrayList<>(r); return this; }
        @Override public BuildStep role(String r)              { roles.add(req(r, "role"));     return this; }
        @Override public BuildStep status(AccountStatus s)     { status        = s;             return this; }
        @Override public BuildStep emailVerified(boolean v)    { emailVerified = v;             return this; }
        @Override public BuildStep bio(String b)               { bio           = b;             return this; }
        @Override public BuildStep avatarBytes(byte[] b)       { avatarBytes   = b != null ? b.clone() : null; return this; }
        @Override public BuildStep updatedNow()                { updatedAt     = LocalDateTime.now(); return this; }

        @Override
        public UserProfile build() {
            return new UserProfile(this);
        }

        private static String req(String v, String field) {
            if (v == null || v.isBlank())
                throw new IllegalArgumentException(field + " is required");
            return v;
        }

        static String validEmail(String e) {
            req(e, "email");
            if (!EMAIL_RE.matcher(e).matches())
                throw new IllegalArgumentException("Invalid email: " + e);
            return e;
        }
    }

    // -------------------------------------------------------------------------
    // Demonstrations
    // -------------------------------------------------------------------------
    static void buildAndCopyDemo() {
        System.out.println("=== UserProfile (Step Builder + immutable + copy) ===");

        UserProfile alice = UserProfile.builder()
            .userId("USR-001")
            .username("alice_sharma")
            .email("alice@example.com")
            .displayName("Alice Sharma")
            .dateOfBirth(LocalDate.of(1993, 6, 15))
            .address(new Address("123 MG Road", "Bengaluru", "Karnataka", "560001"))
            .role("USER")
            .status(AccountStatus.ACTIVE)
            .bio("Software engineer at TechCorp")
            .build();

        System.out.println(alice);

        // Non-destructive updates
        UserProfile verified = alice.verified();
        UserProfile admin    = alice.withRole("ADMIN");
        UserProfile updated  = alice.withStatus(AccountStatus.SUSPENDED);

        System.out.println("Original status: " + alice.status());
        System.out.println("Verified:        " + verified.emailVerified());
        System.out.println("Admin roles:     " + admin.roles());
        System.out.println("Suspended:       " + updated.status());

        // Defensive copy validation
        List<String> externalRoles = alice.roles();
        externalRoles.add("HACKER");
        System.out.println("Original roles after external mutation attempt: " + alice.roles());
    }

    static void validationDemo() {
        System.out.println("\n=== Validation ===");

        try {
            UserProfile.builder()
                .userId("X")
                .username("bob")
                .email("not-an-email")
                .displayName("Bob")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        } catch (IllegalArgumentException e) {
            System.out.println("Bad email: " + e.getMessage());
        }

        try {
            new Address("street", "city", "state", "12345");
        } catch (IllegalArgumentException e) {
            System.out.println("Bad pincode: " + e.getMessage());
        }

        try {
            UserProfile.builder()
                .userId("X")
                .username("bob")
                .email("bob@x.com")
                .displayName("Bob")
                .dateOfBirth(LocalDate.now().plusDays(1)) // future date
                .build();
        } catch (IllegalArgumentException e) {
            System.out.println("Future DOB: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        buildAndCopyDemo();
        validationDemo();
    }
}
