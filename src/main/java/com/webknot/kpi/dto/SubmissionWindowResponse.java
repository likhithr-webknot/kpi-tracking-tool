package com.webknot.kpi.dto;


import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record SubmissionWindowResponse(
        String cycleKey,
        String timezone,
        OffsetDateTime serverNow,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean manualClosed,
        boolean isOpen,
        OffsetDateTime updatedAt,
        String updatedBy
) {}

