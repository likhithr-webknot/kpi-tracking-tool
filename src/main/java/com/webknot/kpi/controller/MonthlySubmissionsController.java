package com.webknot.kpi.controller;

import com.webknot.kpi.service.MonthlySubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monthly-submissions")
public class MonthlySubmissionsController {
    private static final Logger log = LoggerFactory.getLogger(MonthlySubmissionsController.class);

    private final MonthlySubmissionService monthlySubmissionService;

    public MonthlySubmissionsController(MonthlySubmissionService monthlySubmissionService) {
        this.monthlySubmissionService = monthlySubmissionService;
    }

    @PostMapping("/draft")
    public ResponseEntity<?> saveDraft(@RequestBody(required = false) Map<String, Object> body,
                                       Authentication authentication) {
        try {
            return ResponseEntity.ok(monthlySubmissionService.saveDraft(authentication, body));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to save monthly draft", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save draft");
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody(required = false) Map<String, Object> body,
                                    Authentication authentication) {
        try {
            return ResponseEntity.ok(monthlySubmissionService.submit(authentication, body));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to submit monthly submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMine(@RequestParam(required = false) Map<String, String> query,
                                     Authentication authentication) {
        try {
            Map<String, Object> mine = monthlySubmissionService.getMine(authentication, query);
            if (mine == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No submission found");
            return ResponseEntity.ok(mine);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch my monthly submission", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch");
        }
    }

    @GetMapping("/me/history")
    public ResponseEntity<?> getMyHistory(Authentication authentication) {
        try {
            List<Map<String, Object>> history = monthlySubmissionService.getMyHistory(authentication);
            return ResponseEntity.ok(history);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch monthly submission history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch history");
        }
    }

    @GetMapping("/cycles")
    public ResponseEntity<?> getCycleHistory(@RequestParam(required = false) Map<String, String> query,
                                             Authentication authentication) {
        try {
            return ResponseEntity.ok(monthlySubmissionService.getCycleHistory(authentication, query));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch cycle history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch cycle history");
        }
    }

    @GetMapping("/manager/team")
    public ResponseEntity<?> getManagerTeam(@RequestParam(required = false) Map<String, String> query,
                                            Authentication authentication) {
        try {
            return ResponseEntity.ok(monthlySubmissionService.getManagerTeam(authentication, query));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch manager team submissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch manager team");
        }
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAdminAll(@RequestParam(required = false) Map<String, String> query,
                                         Authentication authentication) {
        try {
            return ResponseEntity.ok(monthlySubmissionService.getAdminAll(authentication, query));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch admin submissions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch admin submissions");
        }
    }

    @PostMapping({"/admin/review", "/admin/reviews", "/admin/decision"})
    public ResponseEntity<?> submitAdminReview(@RequestBody(required = false) Map<String, Object> body,
                                               Authentication authentication) {
        try {
            return ResponseEntity.ok(monthlySubmissionService.submitAdminReview(authentication, body));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to submit admin review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit admin review");
        }
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable String id, Authentication authentication) {
        try {
            Long parsedId = Long.parseLong(String.valueOf(id).trim());
            return ResponseEntity.ok(monthlySubmissionService.getAdminById(authentication, parsedId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid id: " + id);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch admin submission by id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch submission");
        }
    }

    @PostMapping("/admin/{id}/reject")
    public ResponseEntity<?> rejectAdminSubmission(@PathVariable String id, 
                                                   @RequestBody(required = false) Map<String, Object> body,
                                                   Authentication authentication) {
        try {
            Long parsedId = Long.parseLong(String.valueOf(id).trim());
            return ResponseEntity.ok(monthlySubmissionService.rejectAdminSubmission(authentication, parsedId, body));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid submission id: " + id);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to reject admin submission id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to reject submission");
        }
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteAdminById(@PathVariable String id, Authentication authentication) {
        try {
            Long parsedId = Long.parseLong(String.valueOf(id).trim());
            return ResponseEntity.ok(monthlySubmissionService.deleteAdminById(authentication, parsedId));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid id: " + id);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete admin submission id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete submission");
        }
    }
}
