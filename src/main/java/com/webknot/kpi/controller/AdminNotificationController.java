package com.webknot.kpi.controller;

import com.webknot.kpi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;

@RestController
@RequestMapping("/admin/notifications")
public class AdminNotificationController {
    private static final Logger log = LoggerFactory.getLogger(AdminNotificationController.class);

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> list(Authentication authentication,
                                  @RequestParam(required = false) String types,
                                  @RequestParam(required = false) Integer limit,
                                  @RequestParam(required = false) String cursor,
                                  @RequestParam(required = false) Boolean unreadOnly) {
        try {
            NotificationService.NotificationPage page = notificationService.listForActor(
                    authentication,
                    types,
                    limit,
                    cursor,
                    unreadOnly,
                    true
            );
            return ResponseEntity.ok(page);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to list admin notifications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to load notifications.");
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(Authentication authentication, @PathVariable("id") String idRaw) {
        try {
            Long id = parseId(idRaw);
            NotificationService.NotificationPayload payload = notificationService.markRead(authentication, id, true);
            return ResponseEntity.ok(payload);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to mark admin notification read id={}", idRaw, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark notification as read.");
        }
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication authentication) {
        try {
            int updated = notificationService.markAllRead(authentication, true);
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("status", "ok");
            body.put("updatedCount", updated);
            return ResponseEntity.ok(body);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to mark all admin notifications as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark notifications as read.");
        }
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication authentication,
                             @RequestParam(required = false) String types) {
        return notificationService.subscribe(authentication, types, true);
    }

    private Long parseId(String idRaw) {
        try {
            return Long.parseLong(String.valueOf(idRaw).trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid id: " + idRaw);
        }
    }
}
