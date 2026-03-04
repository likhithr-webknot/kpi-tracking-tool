package com.webknot.kpi.controller;

import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.models.StreamDirectory;
import com.webknot.kpi.service.DesignationLookupService;
import com.webknot.kpi.service.StreamDirectoryService;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/streams")
public class StreamDirectoryController {

    private static final Logger log = LoggerFactory.getLogger(StreamDirectoryController.class);
    private final StreamDirectoryService streamDirectoryService;
    private final DesignationLookupService designationLookupService;

    public StreamDirectoryController(StreamDirectoryService streamDirectoryService,
                                     DesignationLookupService designationLookupService) {
        this.streamDirectoryService = streamDirectoryService;
        this.designationLookupService = designationLookupService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Map<String, String> query) {
        try {
            Boolean activeOnly = parseBoolean(query.get("activeOnly"));
            Integer limit = parseInt(query.get("limit"));
            String cursor = query.get("cursor");
            StreamDirectoryService.CursorPage page = streamDirectoryService.list(activeOnly, limit, cursor);
            List<StreamDirectoryResponse> items = page.items().stream().map(StreamDirectoryController::toResponse).toList();
            return ResponseEntity.ok(new CursorPageResponse<>(items, page.nextCursor()));
        } catch (Exception e) {
            log.error("Failed to list stream directory with query={}", query, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/designation")
    public ResponseEntity<?> getDesignation(@RequestParam String stream, @RequestParam String band) {
        try {
            Optional<CurrentBand> parsedBand = BandStreamNormalizer.parseBand(band);
            if (parsedBand.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid band: " + band));
            }

            Optional<DesignationLookup> designation = designationLookupService.getByStreamAndBand(stream, parsedBand.get());
            if (designation.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Designation not found for stream=" + stream + ", band=" + band));
            }

            return ResponseEntity.ok(toDesignationResponse(designation.get()));
        } catch (Exception e) {
            log.error("Failed to get designation for stream={}, band={}", stream, band, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody(required = false) Map<String, Object> body,
                                 Authentication authentication) {
        try {
            requireAdmin(authentication);
            StreamDirectory saved = streamDirectoryService.add(
                    firstNonBlank(body, "code", "stream"),
                    firstNonBlankAllowNull(body, "label", "name", "title"),
                    parseBoolean(body == null ? null : body.get("active")),
                    parseIntObj(body == null ? null : body.get("sortOrder"))
            );
            return ResponseEntity.ok(toResponse(saved));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to add stream payload={}", body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{code}")
    public ResponseEntity<?> update(@PathVariable String code,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    Authentication authentication) {
        try {
            requireAdmin(authentication);
            StreamDirectory updated = streamDirectoryService.update(
                    code,
                    firstNonBlankAllowNull(body, "label", "name", "title"),
                    parseBoolean(body == null ? null : body.get("active")),
                    parseIntObj(body == null ? null : body.get("sortOrder"))
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update stream code={} payload={}", code, body, e);
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
            streamDirectoryService.delete(code);
            return ResponseEntity.ok(Map.of("status", "ok", "code", code));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete stream code={}", code, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static StreamDirectoryResponse toResponse(StreamDirectory row) {
        return new StreamDirectoryResponse(
                row.getCode() != null ? row.getCode().name() : null,
                row.getLabel(),
                row.isActive(),
                row.getSortOrder(),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    private static DesignationResponse toDesignationResponse(DesignationLookup designation) {
        return new DesignationResponse(
                designation.getId().getStream(),
                designation.getId().getBand() != null ? designation.getId().getBand().name() : null,
                designation.getDesignation()
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

    public record StreamDirectoryResponse(
            String code,
            String label,
            boolean active,
            Integer sortOrder,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {
    }

    public record DesignationResponse(
            String stream,
            String band,
            String designation
    ) {
    }

    public record CursorPageResponse<T>(List<T> items, String nextCursor) {
    }
}

