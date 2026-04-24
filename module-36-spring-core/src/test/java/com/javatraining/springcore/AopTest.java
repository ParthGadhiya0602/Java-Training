package com.javatraining.springcore;

import com.javatraining.springcore.aop.LoggingAspect;
import com.javatraining.springcore.service.OrderService;
import com.javatraining.springcore.service.ReportService;
import com.javatraining.springcore.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that the LoggingAspect intercepts service calls correctly.
 *
 * <p>Spring AOP works via CGLIB proxies — the injected bean is a proxy
 * that wraps the actual target.  Advice only fires on external method calls
 * routed through the proxy, not on internal this.method() calls.
 */
@SpringBootTest
class AopTest {

    @Autowired LoggingAspect aspect;
    @Autowired UserService   userService;
    @Autowired ReportService reportService;
    @Autowired OrderService  orderService;

    @BeforeEach
    void clearLog() {
        aspect.clearLog();
    }

    // ── @Before and @AfterReturning ───────────────────────────────────────────

    @Test
    void before_advice_fires_on_userService_call() {
        userService.createUser(10L, "Bob");
        assertThat(aspect.getLog())
                .anyMatch(e -> e.startsWith("BEFORE createUser"));
    }

    @Test
    void after_returning_advice_fires_on_successful_return() {
        userService.findUser(99L);
        assertThat(aspect.getLog())
                .anyMatch(e -> e.startsWith("AFTER_RETURNING findUser"));
    }

    @Test
    void after_advice_fires_always_after_method() {
        userService.createUser(11L, "Carol");
        assertThat(aspect.getLog())
                .anyMatch(e -> e.startsWith("AFTER createUser"));
    }

    @Test
    void advice_order_is_before_then_after_returning_then_after() {
        userService.findUser(0L);
        // Filter only the relevant advice entries
        var entries = aspect.getLog().stream()
                .filter(e -> e.contains("findUser"))
                .toList();
        // BEFORE must precede AFTER_RETURNING, which precedes AFTER
        int beforeIdx       = indexOfPrefix(entries, "BEFORE");
        int afterReturnIdx  = indexOfPrefix(entries, "AFTER_RETURNING");
        int afterIdx        = indexOfPrefix(entries, "AFTER findUser");
        assertThat(beforeIdx).isLessThan(afterReturnIdx);
        assertThat(afterReturnIdx).isLessThan(afterIdx);
    }

    // ── @AfterThrowing ────────────────────────────────────────────────────────

    @Test
    void after_throwing_fires_when_service_throws() {
        assertThatThrownBy(() -> orderService.placeOrder("Widget", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(aspect.getLog())
                .anyMatch(e -> e.startsWith("AFTER_THROWING placeOrder"));
    }

    @Test
    void after_throwing_captures_exception_message() {
        assertThatThrownBy(() -> orderService.placeOrder("Widget", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(aspect.getLog())
                .anyMatch(e -> e.contains("Quantity must be positive"));
    }

    @Test
    void exception_still_propagates_after_after_throwing() {
        // @AfterThrowing does NOT swallow the exception
        assertThatThrownBy(() -> orderService.placeOrder("X", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    // ── @Around ───────────────────────────────────────────────────────────────

    @Test
    void around_advice_wraps_report_service_call() {
        reportService.generateReport("Q2");
        assertThat(aspect.getLog())
                .anyMatch(e -> e.startsWith("AROUND_BEFORE generateReport"))
                .anyMatch(e -> e.startsWith("AROUND_AFTER generateReport"));
    }

    @Test
    void around_before_fires_before_around_after() {
        reportService.generateReport("Q3");
        var entries = aspect.getLog().stream()
                .filter(e -> e.contains("generateReport"))
                .toList();
        int beforeIdx = indexOfPrefix(entries, "AROUND_BEFORE");
        int afterIdx  = indexOfPrefix(entries, "AROUND_AFTER");
        assertThat(beforeIdx).isLessThan(afterIdx);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private int indexOfPrefix(java.util.List<String> list, String prefix) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).startsWith(prefix)) return i;
        }
        return -1;
    }
}
