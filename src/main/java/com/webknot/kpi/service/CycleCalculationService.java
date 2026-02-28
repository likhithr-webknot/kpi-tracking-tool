package com.webknot.kpi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for calculating and managing submission cycles.
 * A cycle is identified by a cycle key in the format "yyyy-MM" (e.g., "2026-02").
 */
@Service
public class CycleCalculationService {

    private static final DateTimeFormatter CYCLE_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String DEFAULT_TZ = "Asia/Kolkata";
    private static final Logger log = LogManager.getLogger(CycleCalculationService.class);

    /**
     * Calculates the current cycle key based on the provided timezone.
     *
     * @param timezone the timezone to use for calculation
     * @return the cycle key in format "yyyy-MM"
     */
    public String calculateCurrentCycleKey(String timezone) {
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone;
        ZoneId zone = ZoneId.of(tz);
        ZonedDateTime localNow = ZonedDateTime.now().withZoneSameInstant(zone);
        YearMonth ym = YearMonth.from(localNow);
        return ym.format(CYCLE_FMT);
    }

    /**
     * Calculates the current cycle key using UTC timezone.
     *
     * @return the cycle key in format "yyyy-MM"
     */
    public String calculateCurrentCycleKey() {
        return calculateCurrentCycleKey(null);
    }

    /**
     * Calculates the cycle key for a specific date and timezone.
     *
     * @param dateTime the date/time to calculate the cycle for
     * @param timezone the timezone to use
     * @return the cycle key in format "yyyy-MM"
     */
    public String calculateCycleKey(OffsetDateTime dateTime, String timezone) {
        if (dateTime == null) {
            throw new IllegalArgumentException("dateTime cannot be null");
        }
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone;
        ZoneId zone = ZoneId.of(tz);
        ZonedDateTime zoned = dateTime.atZoneSameInstant(zone);
        YearMonth ym = YearMonth.from(zoned);
        return ym.format(CYCLE_FMT);
    }

    /**
     * Calculates the cycle key for a specific LocalDateTime using a timezone.
     *
     * @param localDateTime the local date/time
     * @param timezone the timezone
     * @return the cycle key in format "yyyy-MM"
     */
    public String calculateCycleKey(LocalDateTime localDateTime, String timezone) {
        if (localDateTime == null) {
            throw new IllegalArgumentException("localDateTime cannot be null");
        }
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone;
        ZoneId zone = ZoneId.of(tz);
        ZonedDateTime zoned = localDateTime.atZone(zone);
        YearMonth ym = YearMonth.from(zoned);
        return ym.format(CYCLE_FMT);
    }

    /**
     * Calculates the next cycle key based on the provided cycle key.
     *
     * @param cycleKey the cycle key in format "yyyy-MM"
     * @return the next cycle key
     * @throws IllegalArgumentException if cycleKey is invalid
     */
    public String calculateNextCycleKey(String cycleKey) {
        YearMonth ym = parseYearMonth(cycleKey);
        YearMonth next = ym.plusMonths(1);
        return next.format(CYCLE_FMT);
    }

    /**
     * Calculates the previous cycle key based on the provided cycle key.
     *
     * @param cycleKey the cycle key in format "yyyy-MM"
     * @return the previous cycle key
     * @throws IllegalArgumentException if cycleKey is invalid
     */
    public String calculatePreviousCycleKey(String cycleKey) {
        YearMonth ym = parseYearMonth(cycleKey);
        YearMonth previous = ym.minusMonths(1);
        return previous.format(CYCLE_FMT);
    }

    /**
     * Calculates a cycle key that is N months from the provided cycle key.
     *
     * @param cycleKey the base cycle key
     * @param monthsOffset positive or negative number of months to offset
     * @return the offset cycle key
     * @throws IllegalArgumentException if cycleKey is invalid
     */
    public String calculateOffsetCycleKey(String cycleKey, int monthsOffset) {
        YearMonth ym = parseYearMonth(cycleKey);
        YearMonth offset = ym.plusMonths(monthsOffset);
        return offset.format(CYCLE_FMT);
    }

    /**
     * Validates if a cycle key is in the correct format "yyyy-MM".
     *
     * @param cycleKey the cycle key to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidCycleKey(String cycleKey) {
        if (cycleKey == null || cycleKey.isBlank()) {
            return false;
        }
        try {
            parseYearMonth(cycleKey);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Gets the start of the cycle (first day of the month) in the specified timezone.
     *
     * @param cycleKey the cycle key
     * @param timezone the timezone
     * @return the start of the cycle as OffsetDateTime
     */
    public OffsetDateTime getCycleStartAt(String cycleKey, String timezone) {
        YearMonth ym = parseYearMonth(cycleKey);
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone;
        ZoneId zone = ZoneId.of(tz);
        
        LocalDateTime startLocal = ym.atDay(1).atStartOfDay();
        ZonedDateTime zoned = startLocal.atZone(zone);
        return zoned.toOffsetDateTime();
    }

    /**
     * Gets the end of the cycle (last day of the month) in the specified timezone.
     *
     * @param cycleKey the cycle key
     * @param timezone the timezone
     * @return the end of the cycle as OffsetDateTime
     */
    public OffsetDateTime getCycleEndAt(String cycleKey, String timezone) {
        YearMonth ym = parseYearMonth(cycleKey);
        String tz = timezone == null || timezone.isBlank() ? DEFAULT_TZ : timezone;
        ZoneId zone = ZoneId.of(tz);
        
        LocalDateTime endLocal = ym.atEndOfMonth().atTime(23, 59, 59);
        ZonedDateTime zoned = endLocal.atZone(zone);
        return zoned.toOffsetDateTime();
    }

    /**
     * Gets the number of days in a cycle (month).
     *
     * @param cycleKey the cycle key
     * @return the number of days in the month
     */
    public int getDaysInCycle(String cycleKey) {
        YearMonth ym = parseYearMonth(cycleKey);
        return ym.lengthOfMonth();
    }

    /**
     * Checks if a given date falls within a cycle.
     *
     * @param cycleKey the cycle key
     * @param dateTime the date/time to check
     * @param timezone the timezone to use
     * @return true if the date is within the cycle, false otherwise
     */
    public boolean isDateInCycle(String cycleKey, OffsetDateTime dateTime, String timezone) {
        if (dateTime == null) {
            return false;
        }
        OffsetDateTime cycleStart = getCycleStartAt(cycleKey, timezone);
        OffsetDateTime cycleEnd = getCycleEndAt(cycleKey, timezone);
        
        return !dateTime.isBefore(cycleStart) && !dateTime.isAfter(cycleEnd);
    }

    /**
     * Calculates a list of cycle keys for a range of cycles.
     *
     * @param startCycleKey the start cycle (inclusive)
     * @param endCycleKey the end cycle (inclusive)
     * @return a list of cycle keys in the range
     */
    public List<String> getCycleRange(String startCycleKey, String endCycleKey) {
        YearMonth start = parseYearMonth(startCycleKey);
        YearMonth end = parseYearMonth(endCycleKey);
        
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startCycleKey must not be after endCycleKey");
        }
        
        List<String> cycles = new ArrayList<>();
        YearMonth current = start;
        while (!current.isAfter(end)) {
            cycles.add(current.format(CYCLE_FMT));
            current = current.plusMonths(1);
        }
        return cycles;
    }

    /**
     * Gets a list of the last N cycles.
     *
     * @param count the number of cycles to return
     * @return a list of cycle keys (most recent first)
     */
    public List<String> getLastNCycles(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        
        List<String> cycles = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < count; i++) {
            cycles.add(current.format(CYCLE_FMT));
            current = current.minusMonths(1);
        }
        return cycles;
    }

    /**
     * Gets a list of the next N cycles.
     *
     * @param count the number of cycles to return
     * @return a list of cycle keys (earliest first)
     */
    public List<String> getNextNCycles(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        
        List<String> cycles = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < count; i++) {
            cycles.add(current.format(CYCLE_FMT));
            current = current.plusMonths(1);
        }
        return cycles;
    }

    /**
     * Calculates the number of months between two cycles.
     *
     * @param fromCycleKey the starting cycle
     * @param toCycleKey the ending cycle
     * @return the number of months between cycles (can be negative)
     */
    public long getMonthsBetweenCycles(String fromCycleKey, String toCycleKey) {
        YearMonth from = parseYearMonth(fromCycleKey);
        YearMonth to = parseYearMonth(toCycleKey);
        return ChronoUnit.MONTHS.between(from, to);
    }

    /**
     * Checks if a cycle is in the past relative to the current month.
     *
     * @param cycleKey the cycle key to check
     * @return true if the cycle is in the past
     */
    public boolean isCycleInPast(String cycleKey) {
        YearMonth ym = parseYearMonth(cycleKey);
        return ym.isBefore(YearMonth.now());
    }

    /**
     * Checks if a cycle is in the future relative to the current month.
     *
     * @param cycleKey the cycle key to check
     * @return true if the cycle is in the future
     */
    public boolean isCycleInFuture(String cycleKey) {
        YearMonth ym = parseYearMonth(cycleKey);
        return ym.isAfter(YearMonth.now());
    }

    /**
     * Checks if a cycle is the current month.
     *
     * @param cycleKey the cycle key to check
     * @return true if the cycle is the current month
     */
    public boolean isCurrentCycle(String cycleKey) {
        YearMonth ym = parseYearMonth(cycleKey);
        return ym.equals(YearMonth.now());
    }

    /**
     * Parses a cycle key string into a YearMonth object.
     *
     * @param cycleKey the cycle key in format "yyyy-MM"
     * @return the YearMonth object
     * @throws IllegalArgumentException if the format is invalid
     */
    private YearMonth parseYearMonth(String cycleKey) {
        if (cycleKey == null || cycleKey.isBlank()) {
            throw new IllegalArgumentException("cycleKey cannot be null or blank");
        }
        try {
            return YearMonth.parse(cycleKey, CYCLE_FMT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                String.format("Invalid cycle key format: '%s'. Expected format: 'yyyy-MM' (e.g., '2026-02')", cycleKey),
                e
            );
        }
    }
}
