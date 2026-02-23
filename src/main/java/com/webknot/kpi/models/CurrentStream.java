package com.webknot.kpi.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.webknot.kpi.util.BandStreamNormalizer;

public enum CurrentStream {
    Development,
    QA,
    Devops,
    DATA,
    UI_UX;

    @JsonCreator
    public static CurrentStream fromValue(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return BandStreamNormalizer.parseStream(raw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid stream: " + raw));
    }
}
