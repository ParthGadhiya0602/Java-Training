package com.javatraining.cicd.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests - no Spring context, no database.
 * Only the semantic-versioning validation logic is exercised here.
 * These are the fastest tests in the CI pipeline.
 */
class ReleaseServiceTest {

    private final ReleaseRepository releaseRepository = mock(ReleaseRepository.class);
    private final ReleaseService    releaseService    = new ReleaseService(releaseRepository);

    @ParameterizedTest
    @ValueSource(strings = {"1.0.0", "2.3.4", "10.20.30", "1.0.0-rc.1", "3.14.0-beta"})
    void create_accepts_valid_semantic_versions(String version) {
        when(releaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Release release = new Release(null, "my-service", version, "2024-01-01");
        Release saved = releaseService.create(release);

        assertThat(saved.getVersion()).isEqualTo(version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0", "1", "v1.0.0", "1.0.0.0", "not-a-version", "", "1.0.0-"})
    void create_rejects_invalid_semantic_versions(String version) {
        Release release = new Release(null, "my-service", version, "2024-01-01");

        assertThatThrownBy(() -> releaseService.create(release))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("semantic versioning");
    }
}
