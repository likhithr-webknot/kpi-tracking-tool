package com.webknot.kpi.controller;

import com.webknot.kpi.service.CycleCalculationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for cycle calculation operations.
 * Provides endpoints for managing and calculating submission cycles.
 */
@RestController
@RequestMapping("/cycles")
public class CycleCalculationController {

    private final CycleCalculationService cycleCalculationService;
    private final Logger log = LogManager.getLogger(CycleCalculationController.class);

    public CycleCalculationController(CycleCalculationService cycleCalculationService) {
        this.cycleCalculationService = cycleCalculationService;
    }

    /**
     * GET /cycles/current
     * Get the current cycle key.
     *
     * @param timezone Optional timezone (defaults to Asia/Kolkata)
     * @return Map with current cycle key
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentCycle(
            @RequestParam(required = false) String timezone) {
        try {
            String cycleKey = timezone == null || timezone.isBlank()
                    ? cycleCalculationService.calculateCurrentCycleKey()
                    : cycleCalculationService.calculateCurrentCycleKey(timezone);
            
            log.info("Retrieved current cycle: {} for timezone: {}", cycleKey, timezone);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("cycleKey", cycleKey);
            response.put("timezone", timezone);
            response.put("timestamp", OffsetDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving current cycle", e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to retrieve current cycle: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/{cycleKey}/next
     * Get the next cycle after the specified cycle.
     *
     * @param cycleKey the base cycle (format: yyyy-MM)
     * @return Map with next cycle key
     */
    @GetMapping("/{cycleKey}/next")
    public ResponseEntity<Map<String, Object>> getNextCycle(@PathVariable String cycleKey) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid cycle key format. Expected: yyyy-MM (e.g., 2026-02)")
                );
            }
            
            String nextCycle = cycleCalculationService.calculateNextCycleKey(cycleKey);
            log.info("Retrieved next cycle: {} for base cycle: {}", nextCycle, cycleKey);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("baseCycleKey", cycleKey);
            response.put("nextCycleKey", nextCycle);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating next cycle for: {}", cycleKey, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to calculate next cycle: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/{cycleKey}/previous
     * Get the previous cycle before the specified cycle.
     *
     * @param cycleKey the base cycle (format: yyyy-MM)
     * @return Map with previous cycle key
     */
    @GetMapping("/{cycleKey}/previous")
    public ResponseEntity<Map<String, Object>> getPreviousCycle(@PathVariable String cycleKey) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid cycle key format. Expected: yyyy-MM (e.g., 2026-02)")
                );
            }
            
            String prevCycle = cycleCalculationService.calculatePreviousCycleKey(cycleKey);
            log.info("Retrieved previous cycle: {} for base cycle: {}", prevCycle, cycleKey);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("baseCycleKey", cycleKey);
            response.put("previousCycleKey", prevCycle);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating previous cycle for: {}", cycleKey, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to calculate previous cycle: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/{cycleKey}/offset
     * Get a cycle that is N months offset from the specified cycle.
     *
     * @param cycleKey the base cycle (format: yyyy-MM)
     * @param months the number of months to offset (positive or negative)
     * @return Map with offset cycle key
     */
    @GetMapping("/{cycleKey}/offset")
    public ResponseEntity<Map<String, Object>> getOffsetCycle(
            @PathVariable String cycleKey,
            @RequestParam int months) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid cycle key format. Expected: yyyy-MM (e.g., 2026-02)")
                );
            }
            
            String offsetCycle = cycleCalculationService.calculateOffsetCycleKey(cycleKey, months);
            log.info("Retrieved offset cycle: {} for base cycle: {} with offset: {}", 
                    offsetCycle, cycleKey, months);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("baseCycleKey", cycleKey);
            response.put("monthsOffset", months);
            response.put("offsetCycleKey", offsetCycle);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating offset cycle for: {}", cycleKey, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to calculate offset cycle: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/{cycleKey}/validate
     * Validate if a cycle key is in the correct format.
     *
     * @param cycleKey the cycle key to validate
     * @return Map with validation result
     */
    @GetMapping("/{cycleKey}/validate")
    public ResponseEntity<Map<String, Object>> validateCycleKey(@PathVariable String cycleKey) {
        boolean isValid = cycleCalculationService.isValidCycleKey(cycleKey);
        log.info("Validated cycle key: {} - Result: {}", cycleKey, isValid);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("cycleKey", cycleKey);
        response.put("isValid", isValid);
        if (!isValid) {
            response.put("expectedFormat", "yyyy-MM (e.g., 2026-02)");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * GET /cycles/{cycleKey}/range
     * Get all cycles within a range from startCycle to endCycle (inclusive).
     *
     * @param cycleKey the start cycle (format: yyyy-MM)
     * @param endCycleKey the end cycle (format: yyyy-MM)
     * @return Map with list of cycles in range
     */
    @GetMapping("/{cycleKey}/range")
    public ResponseEntity<Map<String, Object>> getCycleRange(
            @PathVariable String cycleKey,
            @RequestParam String endCycleKey) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid start cycle key format. Expected: yyyy-MM")
                );
            }
            if (!cycleCalculationService.isValidCycleKey(endCycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid end cycle key format. Expected: yyyy-MM")
                );
            }
            
            List<String> cycles = cycleCalculationService.getCycleRange(cycleKey, endCycleKey);
            log.info("Retrieved cycle range: {} to {} - Count: {}", cycleKey, endCycleKey, cycles.size());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("startCycleKey", cycleKey);
            response.put("endCycleKey", endCycleKey);
            response.put("cycleCount", cycles.size());
            response.put("cycles", cycles);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Invalid cycle range: " + e.getMessage())
            );
        } catch (Exception e) {
            log.error("Error retrieving cycle range", e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to retrieve cycle range: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/last-n
     * Get the last N cycles.
     *
     * @param count the number of cycles to retrieve
     * @return Map with list of last N cycles
     */
    @GetMapping("/last-n")
    public ResponseEntity<Map<String, Object>> getLastNCycles(@RequestParam int count) {
        try {
            if (count <= 0) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Count must be positive")
                );
            }
            
            List<String> cycles = cycleCalculationService.getLastNCycles(count);
            log.info("Retrieved last {} cycles", count);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("requestedCount", count);
            response.put("actualCount", cycles.size());
            response.put("cycles", cycles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving last N cycles", e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to retrieve cycles: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/next-n
     * Get the next N cycles.
     *
     * @param count the number of cycles to retrieve
     * @return Map with list of next N cycles
     */
    @GetMapping("/next-n")
    public ResponseEntity<Map<String, Object>> getNextNCycles(@RequestParam int count) {
        try {
            if (count <= 0) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Count must be positive")
                );
            }
            
            List<String> cycles = cycleCalculationService.getNextNCycles(count);
            log.info("Retrieved next {} cycles", count);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("requestedCount", count);
            response.put("actualCount", cycles.size());
            response.put("cycles", cycles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving next N cycles", e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to retrieve cycles: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/{cycleKey}/info
     * Get detailed information about a cycle.
     *
     * @param cycleKey the cycle key (format: yyyy-MM)
     * @param timezone Optional timezone (defaults to Asia/Kolkata)
     * @return Map with detailed cycle information
     */
    @GetMapping("/{cycleKey}/info")
    public ResponseEntity<Map<String, Object>> getCycleInfo(
            @PathVariable String cycleKey,
            @RequestParam(required = false) String timezone) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid cycle key format. Expected: yyyy-MM (e.g., 2026-02)")
                );
            }
            
            String tz = timezone == null || timezone.isBlank() ? "Asia/Kolkata" : timezone;
            OffsetDateTime startAt = cycleCalculationService.getCycleStartAt(cycleKey, tz);
            OffsetDateTime endAt = cycleCalculationService.getCycleEndAt(cycleKey, tz);
            int daysInCycle = cycleCalculationService.getDaysInCycle(cycleKey);
            boolean isPast = cycleCalculationService.isCycleInPast(cycleKey);
            boolean isFuture = cycleCalculationService.isCycleInFuture(cycleKey);
            boolean isCurrent = cycleCalculationService.isCurrentCycle(cycleKey);
            
            log.info("Retrieved cycle info for: {}", cycleKey);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("cycleKey", cycleKey);
            response.put("timezone", tz);
            response.put("startAt", startAt.toString());
            response.put("endAt", endAt.toString());
            response.put("daysInCycle", daysInCycle);
            response.put("isCurrent", isCurrent);
            response.put("isPast", isPast);
            response.put("isFuture", isFuture);
            response.put("nextCycleKey", cycleCalculationService.calculateNextCycleKey(cycleKey));
            response.put("previousCycleKey", cycleCalculationService.calculatePreviousCycleKey(cycleKey));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving cycle info for: {}", cycleKey, e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to retrieve cycle info: " + e.getMessage())
            );
        }
    }

    /**
     * GET /cycles/{cycleKey}/months-between
     * Calculate the number of months between two cycles.
     *
     * @param cycleKey the from cycle (format: yyyy-MM)
     * @param toCycleKey the to cycle (format: yyyy-MM)
     * @return Map with month difference
     */
    @GetMapping("/{cycleKey}/months-between")
    public ResponseEntity<Map<String, Object>> getMonthsBetween(
            @PathVariable String cycleKey,
            @RequestParam String toCycleKey) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid from cycle key format. Expected: yyyy-MM")
                );
            }
            if (!cycleCalculationService.isValidCycleKey(toCycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid to cycle key format. Expected: yyyy-MM")
                );
            }
            
            long monthsBetween = cycleCalculationService.getMonthsBetweenCycles(cycleKey, toCycleKey);
            log.info("Calculated months between: {} and {} = {}", cycleKey, toCycleKey, monthsBetween);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("fromCycleKey", cycleKey);
            response.put("toCycleKey", toCycleKey);
            response.put("monthsBetween", monthsBetween);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating months between cycles", e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to calculate months between: " + e.getMessage())
            );
        }
    }

    /**
     * POST /cycles/{cycleKey}/check-date
     * Check if a given date falls within a cycle.
     *
     * @param cycleKey the cycle key (format: yyyy-MM)
     * @param dateTime the date/time to check (ISO 8601 format)
     * @param timezone Optional timezone (defaults to Asia/Kolkata)
     * @return Map with date check result
     */
    @PostMapping("/{cycleKey}/check-date")
    public ResponseEntity<Map<String, Object>> checkDateInCycle(
            @PathVariable String cycleKey,
            @RequestParam String dateTime,
            @RequestParam(required = false) String timezone) {
        try {
            if (!cycleCalculationService.isValidCycleKey(cycleKey)) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid cycle key format. Expected: yyyy-MM")
                );
            }
            
            OffsetDateTime odt = OffsetDateTime.parse(dateTime);
            String tz = timezone == null || timezone.isBlank() ? "Asia/Kolkata" : timezone;
            boolean isInCycle = cycleCalculationService.isDateInCycle(cycleKey, odt, tz);
            
            log.info("Checked if date {} is in cycle {}: {}", dateTime, cycleKey, isInCycle);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("cycleKey", cycleKey);
            response.put("dateTime", dateTime);
            response.put("timezone", tz);
            response.put("isInCycle", isInCycle);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking date in cycle", e);
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Failed to check date: " + e.getMessage())
            );
        }
    }
}
