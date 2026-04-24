package com.javatraining.springcore;

import com.javatraining.springcore.lifecycle.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies @PostConstruct is called on startup and the bean is functional.
 *
 * <p>@PreDestroy is verified indirectly: the method exists and runs on
 * context.close() (tested manually; hard to assert in @SpringBootTest
 * without closing the context mid-test).
 */
@SpringBootTest
class BeanLifecycleTest {

    @Autowired
    AuditService auditService;

    @Test
    void post_construct_runs_before_first_use() {
        // @PostConstruct sets initialized = true and adds "INIT" to events
        assertThat(auditService.isInitialized()).isTrue();
        assertThat(auditService.getEvents()).contains("INIT");
    }

    @Test
    void post_construct_is_first_event() {
        assertThat(auditService.getEvents().get(0)).isEqualTo("INIT");
    }

    @Test
    void bean_is_usable_after_lifecycle_init() {
        auditService.record("TEST_EVENT");
        assertThat(auditService.getEvents()).contains("TEST_EVENT");
    }

    @Test
    void pre_destroy_not_yet_called_during_normal_use() {
        // @PreDestroy only fires when the ApplicationContext shuts down
        assertThat(auditService.isDestroyed()).isFalse();
    }
}
