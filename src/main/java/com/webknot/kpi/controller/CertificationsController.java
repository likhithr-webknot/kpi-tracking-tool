package com.webknot.kpi.controller;

import com.webknot.kpi.models.Certification;
import com.webknot.kpi.service.CertificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/certifications")
public class CertificationsController {

    private static final Logger log = LoggerFactory.getLogger(CertificationsController.class);
    private final CertificationService certificationService;

    public CertificationsController(CertificationService certificationService) {
        this.certificationService = certificationService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Map<String, String> query) {
        try {
            Boolean activeOnly = parseBoolean(query.get("activeOnly"));
            Integer limit = parseInt(query.get("limit"));
            String cursor = query.get("cursor");

            boolean paginationRequested = limit != null || (cursor != null && !cursor.isBlank());
            if (paginationRequested) {
                CertificationService.CursorPage page = certificationService.listCursor(activeOnly, limit, cursor);
                List<CertificationResponse> items = page.items().stream()
                        .map(CertificationsController::toResponse)
                        .toList();
                return ResponseEntity.ok(new CursorPageResponse<>(items, page.nextCursor()));
            }

            List<CertificationResponse> items = certificationService.list(activeOnly).stream()
                    .map(CertificationsController::toResponse)
                    .toList();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Failed to list certifications with query={}", query, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody(required = false) Map<String, Object> body) {
        try {
            Certification saved = certificationService.add(
                    firstNonBlank(body, "name", "certificationName", "title"),
                    parseBoolean(body == null ? null : body.get("active"))
            );
            return ResponseEntity.ok(toResponse(saved));
        } catch (Exception e) {
            log.error("Failed to add certification with payload={}", body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            Certification updated = certificationService.update(
                    parseId(id),
                    firstNonBlankAllowNull(body, "name", "certificationName", "title"),
                    parseBoolean(body == null ? null : body.get("active"))
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (Exception e) {
            log.error("Failed to update certification id={} payload={}", id, body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<?> patch(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        return update(id, body);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            certificationService.delete(parseId(id));
            return ResponseEntity.ok(Map.of("status", "ok", "id", id));
        } catch (Exception e) {
            log.error("Failed to delete certification id={}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static CertificationResponse toResponse(Certification c) {
        return new CertificationResponse(
                c.getId(),
                c.getName(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private static Long parseId(String raw) {
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid id: " + raw);
        }
    }

    private static Boolean parseBoolean(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim().toLowerCase();
        if (s.isEmpty()) return null;
        if ("1".equals(s) || "true".equals(s) || "yes".equals(s)) return true;
        if ("0".equals(s) || "false".equals(s) || "no".equals(s)) return false;
        return null;
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid number: " + raw);
        }
    }

    private static String firstNonBlank(Map<String, Object> body, String... keys) {
        String value = firstNonBlankAllowNull(body, keys);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Certification name is required");
        return value;
    }

    private static String firstNonBlankAllowNull(Map<String, Object> body, String... keys) {
        if (body == null || keys == null) return null;
        for (String key : keys) {
            Object raw = body.get(key);
            if (raw == null) continue;
            String s = String.valueOf(raw).trim();
            if (!s.isEmpty()) return s;
        }
        return null;
    }

    public record CertificationResponse(
            Long id,
            String name,
            boolean active,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}

    public record CursorPageResponse<T>(List<T> items, String nextCursor) {}
}
