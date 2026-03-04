package com.webknot.kpi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Profile("prod")
public class ProductionSettingsValidator implements ApplicationRunner {

    private final String jwtSecret;
    private final boolean cookieSecure;
    private final String dbPassword;
    private final boolean permitOpenLegacyEndpoints;
    private final boolean publicDocsEnabled;
    private final boolean publicPrometheusEnabled;
    private final List<String> corsAllowedOriginPatterns;

    public ProductionSettingsValidator(@Value("${jwt.secret:}") String jwtSecret,
                                       @Value("${auth.cookie.secure:false}") boolean cookieSecure,
                                       @Value("${spring.datasource.password:}") String dbPassword,
                                       @Value("${app.security.permit-open-legacy-endpoints:true}") boolean permitOpenLegacyEndpoints,
                                       @Value("${app.security.public-docs-enabled:true}") boolean publicDocsEnabled,
                                       @Value("${app.security.public-prometheus-enabled:false}") boolean publicPrometheusEnabled,
                                       @Value("${app.security.cors.allowed-origin-patterns:}") List<String> corsAllowedOriginPatterns) {
        this.jwtSecret = jwtSecret;
        this.cookieSecure = cookieSecure;
        this.dbPassword = dbPassword;
        this.permitOpenLegacyEndpoints = permitOpenLegacyEndpoints;
        this.publicDocsEnabled = publicDocsEnabled;
        this.publicPrometheusEnabled = publicPrometheusEnabled;
        this.corsAllowedOriginPatterns = corsAllowedOriginPatterns != null ? corsAllowedOriginPatterns : List.of();
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> violations = new ArrayList<>();

        if (jwtSecret == null || jwtSecret.isBlank()) {
            violations.add("jwt.secret must be configured");
        } else {
            String normalized = jwtSecret.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("changeme") || normalized.contains("change-me")) {
                violations.add("jwt.secret must not use placeholder/default values");
            }
            if (jwtSecret.trim().length() < 32) {
                violations.add("jwt.secret must be at least 32 characters");
            }
        }

        if (dbPassword == null || dbPassword.isBlank()) {
            violations.add("spring.datasource.password must be configured");
        }

        if (!cookieSecure) {
            violations.add("auth.cookie.secure must be true");
        }

        if (permitOpenLegacyEndpoints) {
            violations.add("app.security.permit-open-legacy-endpoints must be false");
        }

        if (publicDocsEnabled) {
            violations.add("app.security.public-docs-enabled must be false");
        }

        if (publicPrometheusEnabled) {
            violations.add("app.security.public-prometheus-enabled must be false");
        }

        if (corsAllowedOriginPatterns.isEmpty()) {
            violations.add("app.security.cors.allowed-origin-patterns must include explicit production origins");
        } else {
            boolean hasLocalhostOrigin = corsAllowedOriginPatterns.stream()
                    .filter(origin -> origin != null && !origin.isBlank())
                    .map(origin -> origin.trim().toLowerCase(Locale.ROOT))
                    .anyMatch(origin -> origin.contains("localhost") || origin.contains("127.0.0.1"));
            if (hasLocalhostOrigin) {
                violations.add("app.security.cors.allowed-origin-patterns must not include localhost/127.0.0.1 in prod");
            }
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException("Production readiness checks failed: " + String.join("; ", violations));
        }
    }
}
