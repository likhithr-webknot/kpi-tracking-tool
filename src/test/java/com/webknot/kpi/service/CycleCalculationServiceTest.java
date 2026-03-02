package com.webknot.kpi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CycleCalculationService.
 * Tests all cycle calculation functionality including:
 * - Current cycle calculations
 * - Next/Previous cycle calculations
 * - Cycle key validation
 * - Date range operations
 * - Cycle comparisons
 */
@DisplayName("CycleCalculationService Tests")
public class CycleCalculationServiceTest {

    private CycleCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CycleCalculationService();
    }

    // ==================== Current Cycle Tests ====================

    @Test
    @DisplayName("Should calculate current cycle key in default timezone")
    void testCalculateCurrentCycleKey() {
        String cycleKey = service.calculateCurrentCycleKey();
        assertNotNull(cycleKey);
        assertTrue(service.isValidCycleKey(cycleKey));
        // Format should be yyyy-MM
        assertTrue(cycleKey.matches("\\d{4}-\\d{2}"));
    }

    @Test
    @DisplayName("Should calculate current cycle key for specific timezone")
    void testCalculateCurrentCycleKeyWithTimezone() {
        String cycleKeyUtc = service.calculateCurrentCycleKey("UTC");
        String cycleKeyIndia = service.calculateCurrentCycleKey("Asia/Kolkata");
        assertNotNull(cycleKeyUtc);
        assertNotNull(cycleKeyIndia);
        assertTrue(service.isValidCycleKey(cycleKeyUtc));
        assertTrue(service.isValidCycleKey(cycleKeyIndia));
    }

    // ==================== Cycle Key Validation Tests ====================

    @Test
    @DisplayName("Should validate correct cycle key format")
    void testIsValidCycleKey() {
        assertTrue(service.isValidCycleKey("2026-02"));
        assertTrue(service.isValidCycleKey("2025-01"));
        assertTrue(service.isValidCycleKey("2026-12"));
        assertTrue(service.isValidCycleKey("2020-06"));
    }

    @Test
    @DisplayName("Should reject invalid cycle key formats")
    void testIsInvalidCycleKey() {
        assertFalse(service.isValidCycleKey(null));
        assertFalse(service.isValidCycleKey(""));
        assertFalse(service.isValidCycleKey("  "));
        assertFalse(service.isValidCycleKey("2026-13")); // Invalid month
        assertFalse(service.isValidCycleKey("2026-00")); // Invalid month
        assertFalse(service.isValidCycleKey("26-02"));   // Invalid year format
        assertFalse(service.isValidCycleKey("2026/02")); // Wrong separator
        assertFalse(service.isValidCycleKey("Feb-2026")); // Wrong format
    }

    // ==================== Next/Previous Cycle Tests ====================

    @Test
    @DisplayName("Should calculate next cycle correctly")
    void testCalculateNextCycleKey() {
        assertEquals("2026-03", service.calculateNextCycleKey("2026-02"));
        assertEquals("2026-01", service.calculateNextCycleKey("2025-12"));
        assertEquals("2027-01", service.calculateNextCycleKey("2026-12"));
        assertEquals("2026-02", service.calculateNextCycleKey("2026-01"));
    }

    @Test
    @DisplayName("Should calculate previous cycle correctly")
    void testCalculatePreviousCycleKey() {
        assertEquals("2026-01", service.calculatePreviousCycleKey("2026-02"));
        assertEquals("2025-12", service.calculatePreviousCycleKey("2026-01"));
        assertEquals("2025-11", service.calculatePreviousCycleKey("2025-12"));
        assertEquals("2025-01", service.calculatePreviousCycleKey("2025-02"));
    }

    @Test
    @DisplayName("Should calculate offset cycle correctly")
    void testCalculateOffsetCycleKey() {
        assertEquals("2026-05", service.calculateOffsetCycleKey("2026-02", 3));
        assertEquals("2025-11", service.calculateOffsetCycleKey("2026-02", -3));
        assertEquals("2026-02", service.calculateOffsetCycleKey("2026-02", 0));
        assertEquals("2027-02", service.calculateOffsetCycleKey("2026-02", 12));
    }

    // ==================== Date Range Tests ====================

    @Test
    @DisplayName("Should get cycle start date correctly")
    void testGetCycleStartAt() {
        OffsetDateTime start = service.getCycleStartAt("2026-02", "UTC");
        assertEquals(1, start.getDayOfMonth());
        assertEquals(2, start.getMonthValue());
        assertEquals(2026, start.getYear());
        assertEquals(0, start.getHour());
        assertEquals(0, start.getMinute());
    }

    @Test
    @DisplayName("Should get cycle end date correctly")
    void testGetCycleEndAt() {
        OffsetDateTime end = service.getCycleEndAt("2026-02", "UTC");
        assertEquals(28, end.getDayOfMonth()); // 2026 is not a leap year
        assertEquals(2, end.getMonthValue());
        assertEquals(2026, end.getYear());
        assertEquals(23, end.getHour());
        assertEquals(59, end.getMinute());
    }

    @Test
    @DisplayName("Should get correct number of days in cycle")
    void testGetDaysInCycle() {
        assertEquals(31, service.getDaysInCycle("2026-01"));
        assertEquals(28, service.getDaysInCycle("2026-02")); // Not a leap year
        assertEquals(29, service.getDaysInCycle("2024-02")); // Leap year
        assertEquals(30, service.getDaysInCycle("2026-04"));
        assertEquals(31, service.getDaysInCycle("2026-12"));
    }

    // ==================== Date In Cycle Tests ====================

    @Test
    @DisplayName("Should correctly identify if date is in cycle")
    void testIsDateInCycle() {
        OffsetDateTime dateInCycle = OffsetDateTime.of(2026, 2, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        assertTrue(service.isDateInCycle("2026-02", dateInCycle, "UTC"));
    }

    @Test
    @DisplayName("Should correctly identify if date is not in cycle")
    void testIsDateNotInCycle() {
        OffsetDateTime dateNotInCycle = OffsetDateTime.of(2026, 3, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        assertFalse(service.isDateInCycle("2026-02", dateNotInCycle, "UTC"));
    }

    @Test
    @DisplayName("Should handle null date in isDateInCycle")
    void testIsDateInCycleWithNullDate() {
        assertFalse(service.isDateInCycle("2026-02", null, "UTC"));
    }

    // ==================== Cycle Range Tests ====================

    @Test
    @DisplayName("Should get cycle range correctly")
    void testGetCycleRange() {
        List<String> cycles = service.getCycleRange("2026-01", "2026-03");
        assertEquals(3, cycles.size());
        assertEquals("2026-01", cycles.get(0));
        assertEquals("2026-02", cycles.get(1));
        assertEquals("2026-03", cycles.get(2));
    }

    @Test
    @DisplayName("Should handle single cycle range")
    void testGetCycleRangeSingle() {
        List<String> cycles = service.getCycleRange("2026-02", "2026-02");
        assertEquals(1, cycles.size());
        assertEquals("2026-02", cycles.get(0));
    }

    @Test
    @DisplayName("Should reject invalid cycle range")
    void testGetCycleRangeInvalid() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.getCycleRange("2026-03", "2026-01"));
    }

    // ==================== Last/Next N Cycles Tests ====================

    @Test
    @DisplayName("Should get last N cycles")
    void testGetLastNCycles() {
        List<String> cycles = service.getLastNCycles(3);
        assertEquals(3, cycles.size());
        // Should be in descending order (most recent first)
        String current = service.calculateCurrentCycleKey();
        assertTrue(cycles.get(0).compareTo(current) <= 0);
    }

    @Test
    @DisplayName("Should get next N cycles")
    void testGetNextNCycles() {
        List<String> cycles = service.getNextNCycles(3);
        assertEquals(3, cycles.size());
        // First cycle should be current or later
        String current = service.calculateCurrentCycleKey();
        assertTrue(cycles.get(0).compareTo(current) >= 0);
    }

    @Test
    @DisplayName("Should reject invalid count for getLastNCycles")
    void testGetLastNCyclesInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> service.getLastNCycles(0));
        assertThrows(IllegalArgumentException.class, () -> service.getLastNCycles(-1));
    }

    @Test
    @DisplayName("Should reject invalid count for getNextNCycles")
    void testGetNextNCyclesInvalidCount() {
        assertThrows(IllegalArgumentException.class, () -> service.getNextNCycles(0));
        assertThrows(IllegalArgumentException.class, () -> service.getNextNCycles(-1));
    }

    // ==================== Months Between Tests ====================

    @Test
    @DisplayName("Should calculate months between cycles correctly")
    void testGetMonthsBetweenCycles() {
        assertEquals(2, service.getMonthsBetweenCycles("2026-01", "2026-03"));
        assertEquals(0, service.getMonthsBetweenCycles("2026-02", "2026-02"));
        assertEquals(-2, service.getMonthsBetweenCycles("2026-03", "2026-01"));
        assertEquals(12, service.getMonthsBetweenCycles("2025-02", "2026-02"));
    }

    // ==================== Cycle Comparison Tests ====================

    @Test
    @DisplayName("Should correctly identify past cycles")
    void testIsCycleInPast() {
        String currentCycle = service.calculateCurrentCycleKey();
        String previousCycle = service.calculatePreviousCycleKey(currentCycle);
        assertTrue(service.isCycleInPast(previousCycle));
    }

    @Test
    @DisplayName("Should correctly identify future cycles")
    void testIsCycleInFuture() {
        String currentCycle = service.calculateCurrentCycleKey();
        String nextCycle = service.calculateNextCycleKey(currentCycle);
        assertTrue(service.isCycleInFuture(nextCycle));
    }

    @Test
    @DisplayName("Should correctly identify current cycle")
    void testIsCurrentCycle() {
        String currentCycle = service.calculateCurrentCycleKey();
        assertTrue(service.isCurrentCycle(currentCycle));
    }

    @Test
    @DisplayName("Should return false for non-current cycle")
    void testIsNotCurrentCycle() {
        String previousCycle = service.calculatePreviousCycleKey(service.calculateCurrentCycleKey());
        assertFalse(service.isCurrentCycle(previousCycle));
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("Should throw exception for null cycleKey in calculateNextCycleKey")
    void testCalculateNextCycleKeyNullThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.calculateNextCycleKey(null));
    }

    @Test
    @DisplayName("Should throw exception for invalid cycleKey format")
    void testInvalidCycleKeyFormatThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.calculateNextCycleKey("invalid"));
        assertThrows(IllegalArgumentException.class, 
            () -> service.calculatePreviousCycleKey("2026-13"));
    }

    @Test
    @DisplayName("Should throw exception for null dateTime in calculateCycleKey")
    void testCalculateCycleKeyNullDateTimeThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.calculateCycleKey((OffsetDateTime) null, "UTC"));
    }

    @Test
    @DisplayName("Should throw exception for null localDateTime in calculateCycleKey")
    void testCalculateCycleKeyNullLocalDateTimeThrowsException() {
        assertThrows(IllegalArgumentException.class, 
            () -> service.calculateCycleKey((LocalDateTime) null, "UTC"));
    }
}
