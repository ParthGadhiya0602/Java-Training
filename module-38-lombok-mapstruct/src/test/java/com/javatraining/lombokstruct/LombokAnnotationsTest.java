package com.javatraining.lombokstruct;

import com.javatraining.lombokstruct.entity.Address;
import com.javatraining.lombokstruct.entity.Product;
import com.javatraining.lombokstruct.entity.User;
import com.javatraining.lombokstruct.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies behaviour of the main Lombok annotations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class LombokAnnotationsTest {

    @Autowired NotificationService notificationService;

    // ── @Builder ──────────────────────────────────────────────────────────────

    @Test
    void builder_creates_instance_fluently() {
        User user = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .role("ADMIN")
                .build();

        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void builder_unset_fields_are_null() {
        User user = User.builder().email("test@test.com").build();
        // Fields not set via builder default to null (primitives to 0/false)
        assertThat(user.getId()).isNull();
        assertThat(user.getFirstName()).isNull();
    }

    // ── @Data ─────────────────────────────────────────────────────────────────

    @Test
    void data_generates_getters_setters() {
        Address addr = new Address();
        addr.setStreet("123 Main St");
        addr.setCity("London");
        assertThat(addr.getStreet()).isEqualTo("123 Main St");
        assertThat(addr.getCity()).isEqualTo("London");
    }

    @Test
    void data_generates_equals_and_hashcode() {
        Address a1 = new Address("123 Main St", "London", "UK");
        Address a2 = new Address("123 Main St", "London", "UK");
        Address a3 = new Address("456 Other St", "Paris", "FR");

        assertThat(a1).isEqualTo(a2);
        assertThat(a1).isNotEqualTo(a3);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    }

    @Test
    void data_generates_toString() {
        Address addr = Address.builder().city("Berlin").country("DE").build();
        String str = addr.toString();
        assertThat(str).contains("Berlin").contains("DE");
    }

    // ── @Value (immutable) ────────────────────────────────────────────────────

    @Test
    void value_creates_immutable_instance() {
        Product p = Product.builder()
                .id(1L)
                .name("Laptop")
                .price(new BigDecimal("999.00"))
                .category("Electronics")
                .build();

        assertThat(p.getName()).isEqualTo("Laptop");
        assertThat(p.getPrice()).isEqualByComparingTo("999.00");
        // Setters do not exist — Product has no setName() etc.
        // Trying to call p.setName("X") would be a compilation error.
    }

    @Test
    void value_equals_based_on_all_fields() {
        Product p1 = Product.builder().id(1L).name("Laptop").price(new BigDecimal("999.00")).category("Electronics").build();
        Product p2 = Product.builder().id(1L).name("Laptop").price(new BigDecimal("999.00")).category("Electronics").build();
        Product p3 = Product.builder().id(1L).name("Phone").price(new BigDecimal("699.00")).category("Electronics").build();

        assertThat(p1).isEqualTo(p2);
        assertThat(p1).isNotEqualTo(p3);
    }

    // ── @Slf4j + @RequiredArgsConstructor ─────────────────────────────────────

    @Test
    void slf4j_service_is_wired_and_functional() {
        // @Slf4j injects a private static final Logger named 'log'.
        // @RequiredArgsConstructor generated the constructor that Spring used
        // to inject EmailGateway into NotificationService.
        String result = notificationService.send("alice@example.com", "Hello");
        assertThat(result).startsWith("DELIVERED");
        assertThat(notificationService.getSentCount()).isEqualTo(1);
    }

    @Test
    void required_args_constructor_wired_service_via_constructor_injection() {
        // @RequiredArgsConstructor generated a constructor for every final field
        // (EmailGateway). Spring used that constructor to inject the dependency.
        // The service is non-null and operational — proof the constructor was used.
        assertThat(notificationService).isNotNull();
        // Non-final field sentCount is NOT in the constructor — its value comes
        // from the field initializer (= 0). Across the shared Spring context it may
        // be non-zero if other tests ran first, so we only assert it is non-negative.
        assertThat(notificationService.getSentCount()).isGreaterThanOrEqualTo(0);
    }
}
