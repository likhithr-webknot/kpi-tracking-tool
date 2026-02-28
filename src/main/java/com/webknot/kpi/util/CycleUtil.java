package com.webknot.kpi.util;

import com.webknot.kpi.service.CycleCalculationService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Utility class providing helper methods for cycle-related operations.
 * This complements the CycleCalculationService with convenient utility methods.
 */
@Component
public class CycleUtil {

    private final CycleCalculationService cycleCalculationService;

    public CycleUtil(CycleCalculationService cycleCalculationService) {
        this.cycleCalculationService = cycleCalculationService;
    }

    /**
     * Gets the current cycle key.
     * Example: "2026-02"
     *
     * @return the current cycle key
     */
    public String getCurrentCycle() {
        return cycleCalculationService.calculateCurrentCycleKey();
    }

    /**
     * Gets the current cycle key for a specific timezone.
     * Example with timezone "Asia/Kolkata": "2026-02"
     *
     * @param timezone the timezone string (e.g., "Asia/Kolkata", "America/New_York")
     * @return the current cycle key for the timezone
     */
    public String getCurrentCycle(String timezone) {
        return cycleCalculationService.calculateCurrentCycleKey(timezone);
    }

    /**
     * Gets the next cycle key from the current cycle.
     * If current cycle is "2026-02", returns "2026-03"
     *
     * @return the next cycle key
     */
    public String getNextCycle() {
        String currentCycle = getCurrentCycle();
        return cycleCalculationService.calculateNextCycleKey(currentCycle);
    }

    /**
     * Gets the previous cycle key from the current cycle.
     * If current cycle is "2026-02", returns "2026-01"
     *
     * @return the previous cycle key
     */
    public String getPreviousCycle() {
        String currentCycle = getCurrentCycle();
        return cycleCalculationService.calculatePreviousCycleKey(currentCycle);
    }

    /**
     * Checks if the given cycle is the current month.
     * Example: isCurrent("2026-02") returns true if today is February 2026
     *
     * @param cycleKey the cycle key to check
     * @return true if it's the current cycle
     */
    public boolean isCurrent(String cycleKey) {
        return cycleCalculationService.isCurrentCycle(cycleKey);
    }

    /**
     * Checks if the given cycle is in the past.
     * Example: isPast("2026-01") returns true if current cycle is "2026-02" or later
     *
     * @param cycleKey the cycle key to check
     * @return true if the cycle is in the past
     */
    public boolean isPast(String cycleKey) {
        return cycleCalculationService.isCycleInPast(cycleKey);
    }

    /**
     * Checks if the given cycle is in the future.
     * Example: isFuture("2026-03") returns true if current cycle is "2026-02" or earlier
     *
     * @param cycleKey the cycle key to check
     * @return true if the cycle is in the future
     */
    public boolean isFuture(String cycleKey) {
        return cycleCalculationService.isCycleInFuture(cycleKey);
    }

    /**
     * Validates if a string is a valid cycle key format.
     * Valid format: "yyyy-MM" (e.g., "2026-02")
     *
     * @param cycleKey the potential cycle key
     * @return true if the format is valid
     */
    public boolean isValidFormat(String cycleKey) {
        return cycleCalculationService.isValidCycleKey(cycleKey);
    }

    /**
     * Checks if a specific date falls within a cycle.
     * Example: isDateInCycle("2026-02", someOffsetDateTime, "Asia/Kolkata")
     * returns true if the date is in February 2026 (in Asia/Kolkata timezone)
     *
     * @param cycleKey the cycle key
     * @param dateTime the date/time to check
     * @param timezone the timezone
     * @return true if the date is within the cycle
     */
    public boolean isDateInCycle(String cycleKey, OffsetDateTime dateTime, String timezone) {
        return cycleCalculationService.isDateInCycle(cycleKey, dateTime, timezone);
    }

    /**
     * Gets the number of days in a cycle.
     * Example: getDaysInCycle("2026-02") returns 28
     *
     * @param cycleKey the cycle key
     * @return number of days in the month
     */
    public int getDaysInCycle(String cycleKey) {
        return cycleCalculationService.getDaysInCycle(cycleKey);
    }

    /**
     * Calculates months between two cycles.
     * Example: monthsBetween("2026-01", "2026-03") returns 2
     *
     * @param fromCycleKey the starting cycle
     * @param toCycleKey the ending cycle
     * @return the number of months between cycles
     */
    public long monthsBetween(String fromCycleKey, String toCycleKey) {
        return cycleCalculationService.getMonthsBetweenCycles(fromCycleKey, toCycleKey);
    }
}
