package com.webknot.kpi.controller;


import com.webknot.kpi.dto.ScheduleWindowRequest;
import com.webknot.kpi.dto.SubmissionWindowResponse;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.service.SubmissionWindowService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/submission-window")
public class SubmissionWindowController {

    private final SubmissionWindowService service;
    private final EmployeeRepository employeeRepository;
    private final Logger log = LogManager.getLogger(SubmissionWindowController.class);

    public SubmissionWindowController(SubmissionWindowService service, EmployeeRepository employeeRepository) {
        this.service = service;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/current")
    public SubmissionWindowResponse getCurrent(Authentication authentication) {
        log.info("Fetching current submission window");
        String employeeId = resolveActorEmployeeId(authentication);
        return employeeId != null ? service.getCurrentWindowForEmployee(employeeId) : service.getCurrentWindow();
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/current/schedule")
    public SubmissionWindowResponse schedule(@Valid @RequestBody ScheduleWindowRequest req) {
        log.info("Scheduling submission window");
        return service.scheduleCurrentWindow(req, null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/current/open-now")
    public SubmissionWindowResponse openNow() {
        log.info("Opening submission window immediately");
        return service.openNow(null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/current/close-now")
    public SubmissionWindowResponse closeNow() {
        log.info("Closing submission window immediately");
        return service.closeNow(null);
    }

    @PostMapping("/employee/open-now")
    public ResponseEntity<?> openNowForEmployee(@RequestBody(required = false) Map<String, Object> body,
                                                Authentication authentication) {
        try {
            requireAdmin(authentication);
            Object raw = body == null ? null : body.get("employeeId");
            String employeeId = String.valueOf(raw == null ? "" : raw).trim();
            return ResponseEntity.ok(service.openNowForEmployee(employeeId, resolveActorEmployeeId(authentication)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to open employee submission window", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to open employee window");
        }
    }

    @PostMapping("/employee/{employeeId}/close-now")
    public ResponseEntity<?> closeNowForEmployee(@PathVariable String employeeId, Authentication authentication) {
        try {
            requireAdmin(authentication);
            return ResponseEntity.ok(service.closeNowForEmployee(employeeId, resolveActorEmployeeId(authentication)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to close employee submission window", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to close employee window");
        }
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeWindowStatus(@PathVariable String employeeId, Authentication authentication) {
        try {
            requireAdmin(authentication);
            return ResponseEntity.ok(service.getEmployeeWindowStatus(employeeId));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch employee submission window status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch employee window status");
        }
    }

    private String resolveActorEmployeeId(Authentication authentication) {
        String email = authentication == null ? null : authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) return null;
        return employeeRepository.findByEmail(email).map(Employee::getEmployeeId).orElse(null);
    }

    private static void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equalsIgnoreCase(a.getAuthority()));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }
}
