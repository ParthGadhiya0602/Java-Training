package com.javatraining.springcore;

import com.javatraining.springcore.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies constructor injection, @Primary resolution, and @Qualifier override.
 */
@SpringBootTest
class DependencyInjectionTest {

    @Autowired
    UserService userService;

    // @Primary → EmailNotificationService injected by default
    @Autowired
    NotificationService defaultNotification;

    // @Qualifier overrides @Primary
    @Autowired
    @Qualifier("smsNotificationService")
    NotificationService smsNotification;

    @Autowired
    ReportService reportService;

    @Test
    void userService_constructor_injection_resolves_repository() {
        userService.createUser(1L, "Alice");
        assertThat(userService.findUser(1L)).isPresent().hasValue("Alice");
    }

    @Test
    void missing_user_returns_empty() {
        assertThat(userService.findUser(999L)).isEmpty();
    }

    @Test
    void primary_bean_is_email_notification() {
        assertThat(defaultNotification.getType()).isEqualTo("email");
        assertThat(defaultNotification.notify("hello")).isEqualTo("EMAIL: hello");
    }

    @Test
    void qualifier_selects_sms_over_primary() {
        assertThat(smsNotification.getType()).isEqualTo("sms");
        assertThat(smsNotification.notify("hello")).isEqualTo("SMS: hello");
    }

    @Test
    void report_service_uses_sms_via_setter_injection_and_qualifier() {
        // ReportService injects SMS via setter + @Qualifier
        assertThat(reportService.getNotificationService().getType()).isEqualTo("sms");
        assertThat(reportService.generateReport("Q1"))
                .isEqualTo("Report[Q1] → SMS: Report ready: Q1");
    }
}
