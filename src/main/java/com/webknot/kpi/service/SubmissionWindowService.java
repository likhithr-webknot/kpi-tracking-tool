package com.webknot.kpi.service;


import com.webknot.kpi.dto.ScheduleWindowRequest;
import com.webknot.kpi.dto.SubmissionWindowResponse;
import com.webknot.kpi.models.SubmissionCycle;
import com.webknot.kpi.repository.SubmissionCycleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class SubmissionWindowService {

    private static final DateTimeFormatter CYCLE_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_TZ = "Asia/Kolkata";

    private final SubmissionCycleRepository repo;
    private final Clock clock;
    private final Logger log = LogManager.getLogger(SubmissionWindowService.class);

    @Autowired
    public SubmissionWindowService(SubmissionCycleRepository repo) {
        this(repo, Clock.systemUTC());
    }

    SubmissionWindowService(SubmissionCycleRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    public SubmissionWindowResponse getCurrentWindow() {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);
        log.info("Current submission window fetched for cycle: {}", cycle.getCycleKey());
        return toResponse(cycle, now);
    }

    @Transactional
    public SubmissionWindowResponse scheduleCurrentWindow(ScheduleWindowRequest req, String actorEmployeeId) {
        if (req.endAt() != null && req.endAt().isBefore(req.startAt())) {
            throw new IllegalArgumentException("endAt must be >= startAt");
        }

        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        cycle.setWindowStartAt(req.startAt());
        cycle.setWindowEndAt(req.endAt());
        cycle.setManualClosed(false);
        cycle.setUpdatedBy(actorEmployeeId);

        repo.save(cycle);
        log.info("Submission window scheduled for cycle: {}", cycle.getCycleKey());

        OffsetDateTime now = OffsetDateTime.now(clock);
        return toResponse(cycle, now);
    }

    @Transactional
    public SubmissionWindowResponse openNow(String actorEmployeeId) {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);

        cycle.setWindowStartAt(now);
        cycle.setWindowEndAt(null);
        cycle.setManualClosed(false);
        cycle.setUpdatedBy(actorEmployeeId);

        repo.save(cycle);
        log.info("Submission window opened immediately for cycle: {}", cycle.getCycleKey());
        return toResponse(cycle, now);
    }

    @Transactional
    public SubmissionWindowResponse closeNow(String actorEmployeeId) {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);

        cycle.setWindowEndAt(now);
        cycle.setManualClosed(true);
        cycle.setUpdatedBy(actorEmployeeId);

        repo.save(cycle);
        log.info("Submission window closed immediately for cycle: {}", cycle.getCycleKey());
        return toResponse(cycle, now);
    }

    public boolean isOpenNow(SubmissionCycle cycle) {
        return isOpenAt(cycle, OffsetDateTime.now(clock));
    }

    public boolean isOpenAt(SubmissionCycle cycle, OffsetDateTime now) {
        if (cycle.isManualClosed()) return false;
        if (now.isBefore(cycle.getWindowStartAt())) return false;
        OffsetDateTime end = cycle.getWindowEndAt();
        return end == null || !now.isAfter(end);
    }

    private SubmissionCycle getOrCreateCurrentCycle(String timezone) {
        String cycleKey = currentCycleKey(timezone);
        return repo.findByCycleKey(cycleKey).orElseGet(() -> {
            SubmissionCycle created = new SubmissionCycle();
            created.setCycleKey(cycleKey);
            created.setTimezone(timezone);

            OffsetDateTime now = OffsetDateTime.now(clock);
            OffsetDateTime start = defaultStartAt(now, timezone);
            OffsetDateTime end = start.plusDays(1);

            created.setWindowStartAt(start);
            created.setWindowEndAt(end);
            created.setManualClosed(true);

            log.info("Created default submission cycle: {}", cycleKey);
            return repo.save(created);
        });
    }

    private String currentCycleKey(String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime localNow = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        YearMonth ym = YearMonth.from(localNow);
        return ym.format(CYCLE_FMT);
    }

    private OffsetDateTime defaultStartAt(OffsetDateTime nowUtc, String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime local = nowUtc.atZoneSameInstant(ZoneOffset.UTC).withZoneSameInstant(zone);
        ZonedDateTime startLocal = local.withHour(18).withMinute(0).withSecond(0).withNano(0);
        return startLocal.toOffsetDateTime();
    }

    private SubmissionWindowResponse toResponse(SubmissionCycle cycle, OffsetDateTime serverNow) {
        boolean open = isOpenAt(cycle, serverNow);
        return new SubmissionWindowResponse(
                cycle.getCycleKey(),
                cycle.getTimezone(),
                serverNow,
                cycle.getWindowStartAt(),
                cycle.getWindowEndAt(),
                cycle.isManualClosed(),
                open,
                cycle.getUpdatedAt(),
                cycle.getUpdatedBy()
        );
    }
}
