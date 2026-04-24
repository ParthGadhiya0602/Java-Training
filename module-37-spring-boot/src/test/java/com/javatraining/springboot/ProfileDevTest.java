package com.javatraining.springboot;

import com.javatraining.springboot.config.DatabaseProperties;
import com.javatraining.springboot.profile.DevEnvironmentInfo;
import com.javatraining.springboot.profile.EnvironmentInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with the "dev" profile active.
 *
 * <p>application-dev.properties is merged on top of application.properties:
 * <pre>
 *   app.database.url=jdbc:h2:mem:devdb   (overrides defaultdb)
 *   app.database.pool-size=5             (overrides 10)
 * </pre>
 *
 * <p>{@link DevEnvironmentInfo} is loaded because it carries {@code @Profile("dev")}.
 * {@link com.javatraining.springboot.profile.ProdEnvironmentInfo} is NOT loaded.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class ProfileDevTest {

    @Autowired EnvironmentInfo   environmentInfo;
    @Autowired DatabaseProperties dbProperties;

    @Test
    void dev_profile_loads_DevEnvironmentInfo() {
        assertThat(environmentInfo).isInstanceOf(DevEnvironmentInfo.class);
    }

    @Test
    void dev_environment_name_is_development() {
        assertThat(environmentInfo.getName()).isEqualTo("development");
    }

    @Test
    void dev_environment_has_debug_enabled() {
        assertThat(environmentInfo.isDebugEnabled()).isTrue();
    }

    @Test
    void dev_profile_overrides_database_url() {
        // application-dev.properties: app.database.url=jdbc:h2:mem:devdb
        assertThat(dbProperties.getUrl()).isEqualTo("jdbc:h2:mem:devdb");
    }

    @Test
    void dev_profile_overrides_pool_size() {
        // application-dev.properties: app.database.pool-size=5
        assertThat(dbProperties.getPoolSize()).isEqualTo(5);
    }

    @Test
    void database_url_accessible_via_environment_info() {
        assertThat(environmentInfo.getDatabaseUrl()).contains("devdb");
    }
}
