package com.javatraining.springcore;

import com.javatraining.springcore.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies @Configuration + @Bean registration and ApplicationContext lookup.
 */
@SpringBootTest
class BeanConfigTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    AppProperties appProperties;

    @Test
    void app_properties_bean_registered_via_at_bean() {
        assertThat(appProperties.name()).isEqualTo("Java Training App");
        assertThat(appProperties.version()).isEqualTo("1.0.0");
        assertThat(appProperties.maxConnections()).isEqualTo(100);
    }

    @Test
    void context_contains_expected_beans() {
        assertThat(context.containsBean("userService")).isTrue();
        assertThat(context.containsBean("inMemoryUserRepository")).isTrue();
        assertThat(context.containsBean("emailNotificationService")).isTrue();
        assertThat(context.containsBean("smsNotificationService")).isTrue();
    }

    @Test
    void bean_lookup_by_type_returns_same_singleton() {
        AppProperties a = context.getBean(AppProperties.class);
        AppProperties b = context.getBean(AppProperties.class);
        // Singleton: both references point to the same object
        assertThat(a).isSameAs(b);
    }
}
