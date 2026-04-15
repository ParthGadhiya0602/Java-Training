package com.javatraining.encapsulation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileTest {

    private UserProfile validProfile() {
        return UserProfile.builder()
            .userId("USR-001")
            .username("alice")
            .email("alice@example.com")
            .displayName("Alice Sharma")
            .dateOfBirth(LocalDate.of(1993, 6, 15))
            .status(UserProfile.AccountStatus.ACTIVE)
            .role("USER")
            .build();
    }

    // -----------------------------------------------------------------------
    // Happy-path construction
    // -----------------------------------------------------------------------
    @Test
    void build_sets_required_fields() {
        UserProfile p = validProfile();
        assertEquals("USR-001",           p.userId());
        assertEquals("alice",             p.username());
        assertEquals("alice@example.com", p.email());
        assertEquals("Alice Sharma",      p.displayName());
        assertEquals(UserProfile.AccountStatus.ACTIVE, p.status());
    }

    @Test
    void default_status_is_pending() {
        UserProfile p = UserProfile.builder()
            .userId("X")
            .username("bob")
            .email("bob@x.com")
            .displayName("Bob")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        assertEquals(UserProfile.AccountStatus.PENDING, p.status());
    }

    @Test
    void email_verified_defaults_to_false() {
        assertFalse(validProfile().emailVerified());
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------
    @Test
    void invalid_email_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            UserProfile.builder()
                .userId("X")
                .username("x")
                .email("not-an-email")
                .displayName("X")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());
    }

    @Test
    void future_dob_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            UserProfile.builder()
                .userId("X")
                .username("x")
                .email("x@x.com")
                .displayName("X")
                .dateOfBirth(LocalDate.now().plusDays(1))
                .build());
    }

    @Test
    void blank_user_id_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            UserProfile.builder()
                .userId("")
                .username("x")
                .email("x@x.com")
                .displayName("X")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());
    }

    @Test
    void invalid_address_pincode_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            new UserProfile.Address("st", "city", "state", "12345"));
    }

    // -----------------------------------------------------------------------
    // Immutability / defensive copies
    // -----------------------------------------------------------------------
    @Test
    void mutating_returned_roles_does_not_affect_profile() {
        UserProfile p = validProfile();
        List<String> roles = p.roles();
        roles.add("HACKER");
        assertEquals(1, p.roles().size());
    }

    @Test
    void avatar_bytes_defensive_copy_out() {
        byte[] avatar = {1, 2, 3};
        UserProfile p = UserProfile.builder()
            .userId("X").username("x").email("x@x.com")
            .displayName("X").dateOfBirth(LocalDate.of(1990, 1, 1))
            .avatarBytes(avatar)
            .build();

        byte[] returned = p.avatarBytes();
        returned[0] = 99;                 // mutate returned copy
        assertEquals(1, p.avatarBytes()[0]);  // original unchanged
    }

    @Test
    void avatar_bytes_defensive_copy_in() {
        byte[] avatar = {1, 2, 3};
        UserProfile p = UserProfile.builder()
            .userId("X").username("x").email("x@x.com")
            .displayName("X").dateOfBirth(LocalDate.of(1990, 1, 1))
            .avatarBytes(avatar)
            .build();

        avatar[0] = 99;                   // mutate source after construction
        assertEquals(1, p.avatarBytes()[0]);  // profile's copy unchanged
    }

    // -----------------------------------------------------------------------
    // "with" copy helpers
    // -----------------------------------------------------------------------
    @Test
    void with_status_returns_new_original_unchanged() {
        UserProfile original = validProfile();
        UserProfile suspended = original.withStatus(UserProfile.AccountStatus.SUSPENDED);

        assertEquals(UserProfile.AccountStatus.ACTIVE,    original.status());
        assertEquals(UserProfile.AccountStatus.SUSPENDED, suspended.status());
    }

    @Test
    void verified_returns_new_with_email_verified() {
        UserProfile p = validProfile();
        UserProfile v = p.verified();
        assertFalse(p.emailVerified());
        assertTrue(v.emailVerified());
    }

    @Test
    void with_role_adds_role() {
        UserProfile p     = validProfile();
        UserProfile admin = p.withRole("ADMIN");
        assertFalse(p.hasRole("ADMIN"));
        assertTrue(admin.hasRole("ADMIN"));
        assertTrue(admin.hasRole("USER"));  // existing role preserved
    }

    @Test
    void with_role_does_not_duplicate() {
        UserProfile p = validProfile().withRole("USER");  // USER already exists
        assertEquals(1, p.roles().size());
    }

    @Test
    void without_role_removes_role() {
        UserProfile p        = validProfile();
        UserProfile noUser   = p.withoutRole("USER");
        assertFalse(noUser.hasRole("USER"));
    }

    @Test
    void with_email_validates_new_email() {
        UserProfile p = validProfile();
        assertThrows(IllegalArgumentException.class,
            () -> p.withEmail("not-valid"));
    }

    // -----------------------------------------------------------------------
    // equals / hashCode on userId
    // -----------------------------------------------------------------------
    @Test
    void same_user_id_equals() {
        UserProfile a = validProfile();
        UserProfile b = a.withRole("ADMIN");   // different content, same userId
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void different_user_id_not_equal() {
        UserProfile a = validProfile();
        UserProfile b = UserProfile.builder()
            .userId("USR-002")
            .username("bob")
            .email("bob@example.com")
            .displayName("Bob")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .build();
        assertNotEquals(a, b);
    }

    // -----------------------------------------------------------------------
    // isActive / hasRole helpers
    // -----------------------------------------------------------------------
    @Test
    void is_active_only_when_status_is_active() {
        assertTrue(validProfile().isActive());
        assertFalse(validProfile().withStatus(UserProfile.AccountStatus.SUSPENDED).isActive());
    }

    @Test
    void has_role_false_for_absent_role() {
        assertFalse(validProfile().hasRole("NONEXISTENT"));
    }
}
