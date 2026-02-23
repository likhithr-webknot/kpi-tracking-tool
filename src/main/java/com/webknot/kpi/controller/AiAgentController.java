package com.webknot.kpi.controller;

import com.webknot.kpi.models.AiAgent;
import com.webknot.kpi.service.AiAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai-agents")
public class AiAgentController {

    private static final Logger log = LoggerFactory.getLogger(AiAgentController.class);
    private final AiAgentService aiAgentService;

    public AiAgentController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Map<String, String> query,
                                  Authentication authentication) {
        try {
            requireAdmin(authentication);
            Boolean activeOnly = parseBoolean(query.get("activeOnly"));
            Integer limit = parseInt(query.get("limit"));
            String cursor = query.get("cursor");
            AiAgentService.CursorPage page = aiAgentService.list(activeOnly, limit, cursor);
            List<AiAgentResponse> items = page.items().stream()
                    .map(AiAgentController::toResponse)
                    .toList();
            return ResponseEntity.ok(new CursorPageResponse<>(items, page.nextCursor()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to list AI agents with query={}", query, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody(required = false) Map<String, Object> body,
                                 Authentication authentication) {
        try {
            requireAdmin(authentication);
            AiAgent saved = aiAgentService.add(
                    firstNonBlank(body, "provider"),
                    firstNonBlank(body, "apiKey", "api_key"),
                    parseBoolean(body == null ? null : body.get("active"))
            );
            return ResponseEntity.ok(toResponse(saved));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to add AI agent with payload={}", body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody(required = false) Map<String, Object> body,
                                    Authentication authentication) {
        try {
            requireAdmin(authentication);
            AiAgent updated = aiAgentService.update(
                    parseId(id),
                    firstNonBlankAllowNull(body, "provider"),
                    firstNonBlankAllowNull(body, "apiKey", "api_key"),
                    parseBoolean(body == null ? null : body.get("active"))
            );
            return ResponseEntity.ok(toResponse(updated));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to update AI agent id={} payload={}", id, body, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<?> patch(@PathVariable String id,
                                   @RequestBody(required = false) Map<String, Object> body,
                                   Authentication authentication) {
        return update(id, body, authentication);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication authentication) {
        try {
            requireAdmin(authentication);
            aiAgentService.delete(parseId(id));
            return ResponseEntity.ok(Map.of("status", "ok", "id", id));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete AI agent id={}", id, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/enhance")
    public ResponseEntity<?> enhance(@RequestBody(required = false) Map<String, Object> body) {
        try {
            String text = firstNonBlank(body, "text", "input", "content");
            String mode = firstNonBlankAllowNull(body, "mode", "type");
            AiAgentService.EnhanceResult result = aiAgentService.enhanceReviewText(text, mode);
            return ResponseEntity.ok(Map.of(
                    "text", result.text(),
                    "provider", result.provider(),
                    "model", result.model()
            ));
        } catch (Exception e) {
            log.error("Failed to enhance review text", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/active")
    public ResponseEntity<?> activeAgent() {
        try {
            var active = aiAgentService.getActiveAgentSummary();
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("configured", active.isPresent());
            body.put("provider", active.map(AiAgentService.ActiveAgentSummary::provider).orElse(null));
            body.put("model", active.map(AiAgentService.ActiveAgentSummary::model).orElse(null));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Failed to check active AI agent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to resolve active AI agent.");
        }
    }

    private static void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equalsIgnoreCase(a.getAuthority()));
        if (!isAdmin) throw new AccessDeniedException("Admin access required");
    }

    private static AiAgentResponse toResponse(AiAgent agent) {
        return new AiAgentResponse(
                agent.getId(),
                agent.getProvider(),
                agent.getApiKey(),
                agent.isActive(),
                agent.getCreatedAt(),
                agent.getUpdatedAt()
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

    public record AiAgentResponse(
            Long id,
            String provider,
            String apiKey,
            boolean active,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}

    public record CursorPageResponse<T>(List<T> items, String nextCursor) {}
}
