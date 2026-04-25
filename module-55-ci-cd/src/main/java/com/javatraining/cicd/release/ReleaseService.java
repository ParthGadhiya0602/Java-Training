package com.javatraining.cicd.release;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    /**
     * Semantic versioning pattern: MAJOR.MINOR.PATCH with optional pre-release label.
     * Examples: 1.0.0  2.3.4  1.0.0-rc.1  3.14.0-beta
     */
    private static final Pattern SEMVER =
            Pattern.compile("\\d+\\.\\d+\\.\\d+(-[\\w.]+)?");

    private final ReleaseRepository releaseRepository;

    public List<Release> findAll() {
        return releaseRepository.findAll();
    }

    public Optional<Release> findById(Long id) {
        return releaseRepository.findById(id);
    }

    public Release create(Release release) {
        if (release.getVersion() == null || !SEMVER.matcher(release.getVersion()).matches()) {
            throw new IllegalArgumentException(
                    "Version must follow semantic versioning (e.g. 1.2.3 or 1.2.3-rc.1): "
                    + release.getVersion());
        }
        return releaseRepository.save(release);
    }
}
