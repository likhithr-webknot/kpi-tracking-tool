package com.webknot.kpi.controller;

import com.webknot.kpi.models.WebknotValue;
import com.webknot.kpi.service.WebknotValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webknot-values")
public class WebknotValueController {

    private static final Logger log = LoggerFactory.getLogger(WebknotValueController.class);
    private final WebknotValueService webknotValueService;

    public WebknotValueController(WebknotValueService webknotValueService) {
        this.webknotValueService = webknotValueService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Map<String, String> query) {
        try {
            Boolean activeOnly = parseBoolean(query.get("activeOnly"));
            Integer limit = parseInt(query.get("limit"));
            String cursor = query.get("cursor");
            WebknotValueService.CursorPage page = webknotValueService.list(activeOnly, limit, cursor);
            List<WebknotValueResponse> items = page.items().stream()
                    .map(WebknotValueController::toResponse)
                    .toList();
            return ResponseEntity.ok(new CursorPageResponse<>(items, page.nextCursor()));
        } catch (Exception e) {
            log.error("Failed to list webknot values with query={}", query, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody(required = false) Map<String, Object> body) {
        try {
            WebknotValue saved = webknotValueService.add(
                    firstNonBlank(body, "title", "valueTitle", "name", "label"),
                    firstNonBlankAllowNull(body, "pillar", "valuePillar", "valuePillarName", "pillarName"),
                    firstNonBlankAllowNull(body, "description", "valueDescription", "desc", "details"),
                    parseBoolean(body == null ? null : body.get("active"))
            );
            return ResponseEntity.ok(toResponse(saved));
        } catch (Exception e) {
            log.error("Failed to add webknot value payload={}", body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            WebknotValue updated = webknotValueService.update(
                    parseId(id),
                    firstNonBlankAllowNull(body, "title", "valueTitle", "name", "label"),
                    firstNonBlankAllowNull(body, "pillar", "valuePillar", "valuePillarName", "pillarName"),
                    firstNonBlankAllowNull(body, "description", "valueDescription", "desc", "details"),
                    parseBoolean(body == null ? null : body.get("active"))
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (Exception e) {
            log.error("Failed to update webknot value id={} payload={}", id, body, e);
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
            webknotValueService.delete(parseId(id));
            return ResponseEntity.ok(Map.of("status", "ok", "id", id));
        } catch (Exception e) {
            log.error("Failed to delete webknot value id={}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static WebknotValueResponse toResponse(WebknotValue value) {
        return new WebknotValueResponse(
                value.getId(),
                value.getTitle(),
                value.getPillar(),
                value.getDescription(),
                value.isActive(),
                value.getCreatedAt(),
                value.getUpdatedAt()
        );
    }

    private static Long parseId(String raw) {
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid id: " + raw);
        }
    }

    private static Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid number: " + raw);
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

    private static String firstNonBlank(Map<String, Object> body, String... keys) {
        String value = firstNonBlankAllowNull(body, keys);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Webknot value title is required");
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

    public record CursorPageResponse<T>(List<T> items, String nextCursor) {}

    public record WebknotValueResponse(
            Long id,
            String title,
            String pillar,
            String description,
            boolean active,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}
