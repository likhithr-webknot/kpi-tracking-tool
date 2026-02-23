package com.webknot.kpi.util;

import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;

import java.util.Locale;
import java.util.Optional;

public final class BandStreamNormalizer {

    private BandStreamNormalizer() {
    }

    public static Optional<CurrentBand> parseBand(String raw) {
        String key = normalizeBandKey(raw);
        if (key.isBlank()) return Optional.empty();
        for (CurrentBand band : CurrentBand.values()) {
            if (normalizeBandKey(band.name()).equals(key)) {
                return Optional.of(band);
            }
        }
        return Optional.empty();
    }

    public static Optional<CurrentStream> parseStream(String raw) {
        String key = normalizeStreamKey(raw);
        if (key.isBlank()) return Optional.empty();
        return switch (key) {
            case "development", "dev", "backend", "frontend", "mobile", "fullstack", "engineering" ->
                    Optional.of(CurrentStream.Development);
            case "qa", "qualityassurance", "qualityengineering", "testing", "test" ->
                    Optional.of(CurrentStream.QA);
            case "devops", "devsecops", "sre", "ops", "operations" ->
                    Optional.of(CurrentStream.Devops);
            case "data", "datascience", "analytics", "ai", "ml", "aiml" ->
                    Optional.of(CurrentStream.DATA);
            case "uiux", "uxui", "ui", "ux", "design", "uidesign", "uxdesign" ->
                    Optional.of(CurrentStream.UI_UX);
            default -> Optional.empty();
        };
    }

    public static String canonicalStreamLabel(CurrentStream stream) {
        if (stream == null) return null;
        return stream.name();
    }

    public static String canonicalStreamLabel(String raw) {
        return parseStream(raw)
                .map(BandStreamNormalizer::canonicalStreamLabel)
                .orElse(raw == null ? null : raw.trim());
    }

    private static String normalizeBandKey(String raw) {
        if (raw == null) return "";
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String normalizeStreamKey(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}

