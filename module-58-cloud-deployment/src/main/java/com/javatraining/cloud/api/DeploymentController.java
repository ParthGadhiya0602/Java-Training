package com.javatraining.cloud.api;

import com.javatraining.cloud.config.AppProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes deployment metadata — useful for verifying which version and environment
 * is running without accessing the filesystem or shell.
 *
 * 12-factor Factor XI — Logs / Factor X — Dev/prod parity:
 * The same image runs in every environment; only environment variables differ.
 * This endpoint makes the active config observable.
 */
@RestController
@RequestMapping("/api/deployment")
public class DeploymentController {

    private final AppProperties appProperties;

    public DeploymentController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("name", appProperties.name());
        info.put("version", appProperties.version());
        info.put("environment", appProperties.environment());
        info.put("region", appProperties.region() != null ? appProperties.region() : "unknown");
        return info;
    }
}
