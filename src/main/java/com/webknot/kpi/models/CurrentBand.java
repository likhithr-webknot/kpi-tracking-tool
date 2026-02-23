package com.webknot.kpi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.webknot.kpi.util.BandStreamNormalizer;

import java.util.Optional;

public enum CurrentBand {
    B8,
    B7L,
    B7H,
    B6L,
    B6H,
    B5L,
    B5,
    B5H,
    B4,
    B3,
    B2,
    B1
    ;

    @JsonCreator
    public static CurrentBand fromValue(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return BandStreamNormalizer.parseBand(raw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid band: " + raw));
    }

    public Optional<CurrentBand> oneBandAbove() {
        return switch (this) {
            case B8 -> Optional.of(B7L);
            case B7L -> Optional.of(B7H);
            case B7H -> Optional.of(B6L);
            case B6L -> Optional.of(B6H);
            case B6H -> Optional.of(B5L);
            case B5L -> Optional.of(B5);
            case B5 -> Optional.of(B5H);
            case B5H -> Optional.of(B4);
            case B4 -> Optional.of(B3);
            case B3 -> Optional.of(B2);
            case B2 -> Optional.of(B1);
            case B1 -> Optional.empty();
        };
    }
}
