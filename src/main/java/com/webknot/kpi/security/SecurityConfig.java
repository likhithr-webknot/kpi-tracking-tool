package com.webknot.kpi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final boolean permitOpenLegacyEndpoints;
    private final boolean publicDocsEnabled;
    private final boolean publicPrometheusEnabled;
    private final List<String> corsAllowedOriginPatterns;
    private final List<String> corsAllowedMethods;
    private final List<String> corsAllowedHeaders;
    private final List<String> corsExposedHeaders;
    private final boolean corsAllowCredentials;
    private final long corsMaxAgeSeconds;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Value("${app.security.permit-open-legacy-endpoints:true}") boolean permitOpenLegacyEndpoints,
                          @Value("${app.security.public-docs-enabled:true}") boolean publicDocsEnabled,
                          @Value("${app.security.public-prometheus-enabled:false}") boolean publicPrometheusEnabled,
                          @Value("${app.security.cors.allowed-origin-patterns:http://localhost:3000,http://127.0.0.1:3000}") List<String> corsAllowedOriginPatterns,
                          @Value("${app.security.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") List<String> corsAllowedMethods,
                          @Value("${app.security.cors.allowed-headers:Authorization,Content-Type,X-XSRF-TOKEN,X-CSRF-TOKEN,X-Requested-With,Accept,Origin}") List<String> corsAllowedHeaders,
                          @Value("${app.security.cors.exposed-headers:Set-Cookie,Authorization,X-Request-Id}") List<String> corsExposedHeaders,
                          @Value("${app.security.cors.allow-credentials:true}") boolean corsAllowCredentials,
                          @Value("${app.security.cors.max-age-seconds:3600}") long corsMaxAgeSeconds) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.permitOpenLegacyEndpoints = permitOpenLegacyEndpoints;
        this.publicDocsEnabled = publicDocsEnabled;
        this.publicPrometheusEnabled = publicPrometheusEnabled;
        this.corsAllowedOriginPatterns = sanitizeList(corsAllowedOriginPatterns);
        this.corsAllowedMethods = sanitizeList(corsAllowedMethods);
        this.corsAllowedHeaders = sanitizeList(corsAllowedHeaders);
        this.corsExposedHeaders = sanitizeList(corsExposedHeaders);
        this.corsAllowCredentials = corsAllowCredentials;
        this.corsMaxAgeSeconds = corsMaxAgeSeconds;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .cacheControl(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicy(policy -> policy.policy("geolocation=(), microphone=(), camera=()"))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/auth/**").permitAll();

                    if (permitOpenLegacyEndpoints) {
                        auth.requestMatchers("/employees/**").permitAll();
                        auth.requestMatchers("/submission-window/**").permitAll();
                        auth.requestMatchers("/kpi-definitions/**").permitAll();
                    }

                    auth.requestMatchers(HttpMethod.GET, "/bands/list").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/bands/designation").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/streams/list").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/streams/designation").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/certifications/list").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/webknot-values/list").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/employee-portal/**").permitAll();

                    if (publicDocsEnabled) {
                        auth.requestMatchers("/v3/api-docs/**").permitAll();
                        auth.requestMatchers("/swagger-ui.html").permitAll();
                        auth.requestMatchers("/swagger-ui/**").permitAll();
                    }

                    auth.requestMatchers("/actuator/health").permitAll();
                    auth.requestMatchers("/actuator/info").permitAll();
                    if (publicPrometheusEnabled) {
                        auth.requestMatchers("/actuator/prometheus").permitAll();
                    } else {
                        auth.requestMatchers("/actuator/prometheus").hasRole("Admin");
                    }

                    auth.requestMatchers("/admin/imports/**").hasRole("Admin");
                    auth.requestMatchers("/portal/employee/**").hasRole("Employee");
                    auth.requestMatchers("/portal/manager/**").hasRole("Manager");
                    auth.requestMatchers("/portal/admin/**").hasRole("Admin");
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(corsAllowedOriginPatterns);
        config.setAllowedMethods(corsAllowedMethods);
        config.setAllowedHeaders(corsAllowedHeaders);
        config.setExposedHeaders(corsExposedHeaders);
        config.setMaxAge(corsMaxAgeSeconds);
        config.setAllowCredentials(corsAllowCredentials);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> sanitizeList(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : rawValues) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }
}
