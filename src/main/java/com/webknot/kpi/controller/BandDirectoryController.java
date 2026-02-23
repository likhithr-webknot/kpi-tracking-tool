package com.webknot.kpi.controller;

import com.webknot.kpi.models.BandDirectory;
import com.webknot.kpi.service.BandDirectoryService;
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
@RequestMapping("/bands")
public class BandDirectoryController {

    private static final Logger log = LoggerFactory.getLogger(BandDirectoryController.class);
    private final BandDirectoryService bandDirectoryService;

    public BandDirectoryController(BandDirectoryService bandDirectoryService) {
        this.bandDirectoryService = bandDirectoryService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Map<String, String> query) {
        try {
            Boolean activeOnly = parseBoolean(query.get("activeOnly"));
            Integer limit = parseInt(query.get("limit"));
            String cursor = query.get("cursor");
            BandDirectoryService.CursorPage page = bandDirectoryService.list(activeOnly, limit, cursor);
            List<BandDirectoryResponse> items = page.items().stream().map(BandDirectoryController::toResponse).toList();
            return ResponseEntity.ok(new CursorPageResponse<>(items, page.nextCursor()));
        } catch (Exception e) {
            log.error("Failed to list band directory with query={}", query, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody(required = false) Map<String, Object> body,
                                 Authentication authentication) {
        try {
            requireAdmin(authentication);
            BandDirectory saved = bandDirectoryService.add(
                    firstNonBlank(body, "code", "band"),
                    firstNonBlankAllowNull(body, "label", "name", "title"),
                    parseBoolean(body == null ? null : body.get("active")),
                    parseIntObj(body == null ? null : body.get("sortOrder"))
            );
            return ResponseEntity.ok(toResponse(saved));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to add band payload={}", body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{code}")
    public ResponseEntity<?> update(@PathVariable String code,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    Authentication authentication) {
        try {
            requireAdmin(authentication);
            BandDirectory updated = bandDirectoryService.update(
                    code,
                    firstNonBlankAllowNull(body, "label", "name", "title"),
                    parseBoolean(body == null ? null : body.get("active")),
                    parseIntObj(body == null ? null : body.get("sortOrder"))
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update band code={} payload={}", code, body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/update/{code}")
    public ResponseEntity<?> patch(@PathVariable String code,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   Authentication authentication) {
        return update(code, body, authentication);
    }

    @DeleteMapping("/delete/{code}")
    public ResponseEntity<?> delete(@PathVariable String code, Authentication authentication) {
        try {
            requireAdmin(authentication);
            bandDirectoryService.delete(code);
            return ResponseEntity.ok(Map.of("status", "ok", "code", code));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete band code={}", code, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static BandDirectoryResponse toResponse(BandDirectory row) {
        return new BandDirectoryResponse(
                row.getCode() != null ? row.getCode().name() : null,
                row.getLabel(),
                row.isActive(),
                row.getSortOrder(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    private static void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equalsIgnoreCase(a.getAuthority()));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
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

    private static Integer parseIntObj(Object raw) {
        if (raw == null) return null;
        return parseInt(String.valueOf(raw));
    }

    private static String firstNonBlank(Map<String, Object> body, String... keys) {
        String value = firstNonBlankAllowNull(body, keys);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required field is missing");
        }
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

    public record BandDirectoryResponse(
            String code,
            String label,
            boolean active,
            Integer sortOrder,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {
    }

    public record CursorPageResponse<T>(List<T> items, String nextCursor) {
    }
}

