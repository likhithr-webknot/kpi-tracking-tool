package com.webknot.kpi.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ScheduleWindowRequest(
        @NotNull OffsetDateTime startAt,
        OffsetDateTime endAt
) {}
