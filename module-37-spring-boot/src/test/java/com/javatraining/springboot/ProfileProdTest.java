package com.javatraining.springboot;

import com.javatraining.springboot.config.DatabaseProperties;
import com.javatraining.springboot.profile.EnvironmentInfo;
import com.javatraining.springboot.profile.ProdEnvironmentInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with the "prod" profile active.
 *
 * <p>application-prod.properties overrides:
 * <pre>
 *   app.database.url=jdbc:postgresql://prod-host:5432/proddb
 *   app.database.pool-size=50
 * </pre>
 *
 * <p>No actual database connection is made — DatabaseProperties is purely
 * a typed value holder. The PostgreSQL URL is just a string here.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("prod")
class ProfileProdTest {

    @Autowired EnvironmentInfo    environmentInfo;
    @Autowired DatabaseProperties dbProperties;

    @Test
    void prod_profile_loads_ProdEnvironmentInfo() {
        assertThat(environmentInfo).isInstanceOf(ProdEnvironmentInfo.class);
    }

    @Test
    void prod_environment_name_is_production() {
        assertThat(environmentInfo.getName()).isEqualTo("production");
    }

    @Test
    void prod_environment_has_debug_disabled() {
        assertThat(environmentInfo.isDebugEnabled()).isFalse();
    }

    @Test
    void prod_profile_overrides_database_url() {
        assertThat(dbProperties.getUrl()).isEqualTo("jdbc:postgresql://prod-host:5432/proddb");
    }

    @Test
    void prod_profile_overrides_pool_size() {
        assertThat(dbProperties.getPoolSize()).isEqualTo(50);
    }
}
