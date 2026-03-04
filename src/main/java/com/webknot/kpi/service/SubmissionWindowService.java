package com.webknot.kpi.service;


import com.webknot.kpi.dto.ScheduleWindowRequest;
import com.webknot.kpi.dto.SubmissionWindowResponse;
import com.webknot.kpi.models.EmployeeSubmissionWindowOverride;
import com.webknot.kpi.models.SubmissionCycle;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.repository.EmployeeSubmissionWindowOverrideRepository;
import com.webknot.kpi.repository.SubmissionCycleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SubmissionWindowService {

    private static final DateTimeFormatter CYCLE_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_TZ = "Asia/Kolkata";

    private final SubmissionCycleRepository repo;
    private final EmployeeSubmissionWindowOverrideRepository overrideRepository;
    private final EmployeeRepository employeeRepository;
    private final CycleCalculationService cycleCalculationService;
    private final Clock clock;
    private final Logger log = LogManager.getLogger(SubmissionWindowService.class);

    @Autowired
    public SubmissionWindowService(SubmissionCycleRepository repo,
                                   EmployeeSubmissionWindowOverrideRepository overrideRepository,
                                   EmployeeRepository employeeRepository,
                                   CycleCalculationService cycleCalculationService) {
        this(repo, overrideRepository, employeeRepository, cycleCalculationService, Clock.systemUTC());
    }

    SubmissionWindowService(SubmissionCycleRepository repo,
                            EmployeeSubmissionWindowOverrideRepository overrideRepository,
                            EmployeeRepository employeeRepository,
                            CycleCalculationService cycleCalculationService,
                            Clock clock) {
        this.repo = repo;
        this.overrideRepository = overrideRepository;
        this.employeeRepository = employeeRepository;
        this.cycleCalculationService = cycleCalculationService;
        this.clock = clock;
    }

    public SubmissionWindowResponse getCurrentWindow() {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);
        log.info("Current submission window fetched for cycle: {}", cycle.getCycleKey());
        return toResponse(cycle, now);
    }

    public SubmissionWindowResponse getCurrentWindowForEmployee(String employeeId) {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean open = isOpenForEmployee(cycle, employeeId, now);
        return new SubmissionWindowResponse(
                cycle.getCycleKey(),
                cycle.getTimezone(),
                now,
                cycle.getWindowStartAt(),
                cycle.getWindowEndAt(),
                cycle.isManualClosed(),
                open,
                cycle.getUpdatedAt(),
                cycle.getUpdatedBy()
        );
    }

    @Transactional(timeout = 10)
    public SubmissionWindowResponse scheduleCurrentWindow(ScheduleWindowRequest req, String actorEmployeeId) {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        cycle.setWindowStartAt(req.startAt());
        cycle.setWindowEndAt(req.endAt());
        cycle.setManualClosed(false);
        cycle.setUpdatedBy(actorEmployeeId);
        repo.save(cycle);
        
        OffsetDateTime now = OffsetDateTime.now(clock);
        log.info("Submission window scheduled for cycle={}, start={}, end={}, actor={}",
                cycle.getCycleKey(), req.startAt(), req.endAt(), actorEmployeeId);
        return toResponse(cycle, now);
    }

    @Transactional(timeout = 10)
    public SubmissionWindowResponse openNow(String actorEmployeeId) {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);
        cycle.setWindowStartAt(now);
        cycle.setWindowEndAt(null);
        cycle.setManualClosed(false);
        cycle.setUpdatedBy(actorEmployeeId);
        repo.save(cycle);
        
        log.info("Submission window opened immediately for cycle={}, actor={}", cycle.getCycleKey(), actorEmployeeId);
        return toResponse(cycle, now);
    }

    @Transactional(timeout = 10)
    public SubmissionWindowResponse closeNow(String actorEmployeeId) {
        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);
        cycle.setManualClosed(true);
        cycle.setUpdatedBy(actorEmployeeId);
        repo.save(cycle);
        
        log.info("Submission window closed immediately for cycle={}, actor={}", cycle.getCycleKey(), actorEmployeeId);
        return toResponse(cycle, now);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> openNowForEmployee(String employeeId, String actorEmployeeId) {
        String id = normalizeEmployeeId(employeeId);
        ensureEmployeeExists(id);

        EmployeeSubmissionWindowOverride override = overrideRepository.findByEmployeeId(id)
                .orElseGet(EmployeeSubmissionWindowOverride::new);
        override.setEmployeeId(id);
        override.setForceOpen(true);
        override.setForceClosed(false);
        override.setUpdatedBy(actorEmployeeId);
        overrideRepository.save(override);

        log.info("Submission window opened for employee={}, actor={}", id, actorEmployeeId);
        return getEmployeeWindowStatus(id);
    }

    @Transactional(timeout = 10)
    public Map<String, Object> closeNowForEmployee(String employeeId, String actorEmployeeId) {
        String id = normalizeEmployeeId(employeeId);
        ensureEmployeeExists(id);

        EmployeeSubmissionWindowOverride override = overrideRepository.findByEmployeeId(id)
                .orElseGet(EmployeeSubmissionWindowOverride::new);
        override.setEmployeeId(id);
        override.setForceOpen(false);
        override.setForceClosed(true);
        override.setUpdatedBy(actorEmployeeId);
        overrideRepository.save(override);

        log.info("Submission window closed for employee={}, actor={}", id, actorEmployeeId);
        return getEmployeeWindowStatus(id);
    }

    public Map<String, Object> getEmployeeWindowStatus(String employeeId) {
        String id = normalizeEmployeeId(employeeId);
        ensureEmployeeExists(id);

        SubmissionCycle cycle = getOrCreateCurrentCycle(DEFAULT_TZ);
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean globalOpen = isOpenAt(cycle, now);
        boolean employeeOpen = isOpenForEmployee(cycle, id, now);
        Optional<EmployeeSubmissionWindowOverride> overrideOpt = overrideRepository.findById(id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("employeeId", id);
        body.put("cycleKey", cycle.getCycleKey());
        body.put("globalOpen", globalOpen);
        body.put("windowOpenForEmployee", employeeOpen);
        body.put("employeeWindowOpen", employeeOpen);
        body.put("isOpenForEmployee", employeeOpen);
        body.put("forceOpen", overrideOpt.map(EmployeeSubmissionWindowOverride::isForceOpen).orElse(false));
        body.put("forceClosed", overrideOpt.map(EmployeeSubmissionWindowOverride::isForceClosed).orElse(false));
        body.put("overrideUpdatedAt", overrideOpt.map(EmployeeSubmissionWindowOverride::getUpdatedAt).map(OffsetDateTime::toString).orElse(null));
        body.put("overrideUpdatedBy", overrideOpt.map(EmployeeSubmissionWindowOverride::getUpdatedBy).orElse(null));
        body.put("serverNow", now.toString());
        return body;
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

    public boolean isOpenForEmployee(SubmissionCycle cycle, String employeeId, OffsetDateTime now) {
        boolean globalOpen = isOpenAt(cycle, now);
        if (employeeId == null || employeeId.isBlank()) return globalOpen;

        Optional<EmployeeSubmissionWindowOverride> overrideOpt = overrideRepository.findById(employeeId.trim());
        if (overrideOpt.isEmpty()) return globalOpen;

        EmployeeSubmissionWindowOverride override = overrideOpt.get();
        if (override.isForceOpen()) return true;
        if (override.isForceClosed()) return false;
        return globalOpen;
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
        return cycleCalculationService.calculateCurrentCycleKey(timezone);
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

    private String normalizeEmployeeId(String employeeId) {
        String id = employeeId == null ? "" : employeeId.trim();
        if (id.isBlank()) throw new IllegalArgumentException("employeeId is required");
        return id;
    }

    private void ensureEmployeeExists(String employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new IllegalArgumentException("Employee not found: " + employeeId);
        }
    }
}
