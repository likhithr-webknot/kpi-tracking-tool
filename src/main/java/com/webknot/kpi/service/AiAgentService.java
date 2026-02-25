package com.webknot.kpi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webknot.kpi.models.AiAgent;
import com.webknot.kpi.repository.AiAgentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiAgentService {
    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);
    private static final int DEFAULT_CURSOR_LIMIT = 10;
    private static final int MAX_CURSOR_LIMIT = 100;

    private final AiAgentRepository aiAgentRepository;
    private final ObjectMapper objectMapper;
    private final String defaultEnhanceModel;
    private final HttpClient httpClient;

    public AiAgentService(AiAgentRepository aiAgentRepository,
                          ObjectMapper objectMapper,
                          @Value("${ai.enhance.model:gpt-4o-mini}") String defaultEnhanceModel) {
        this.aiAgentRepository = aiAgentRepository;
        this.objectMapper = objectMapper;
        this.defaultEnhanceModel = defaultEnhanceModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Transactional(readOnly = true)
    public CursorPage list(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeCursorLimit(limit);
        Long startAfter = parseCursorId(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        boolean filterActive = activeOnly != null;

        List<AiAgent> rows;
        if (filterActive) {
            boolean active = activeOnly;
            rows = startAfter == null
                    ? aiAgentRepository.findByActiveOrderByIdAsc(active, pageable)
                    : aiAgentRepository.findByActiveAndIdGreaterThanOrderByIdAsc(active, startAfter, pageable);
        } else {
            rows = startAfter == null
                    ? aiAgentRepository.findAllByOrderByIdAsc(pageable)
                    : aiAgentRepository.findByIdGreaterThanOrderByIdAsc(startAfter, pageable);
        }

        boolean hasMore = rows.size() > pageSize;
        List<AiAgent> items = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasMore && !items.isEmpty()
                ? String.valueOf(items.get(items.size() - 1).getId())
                : null;

        return new CursorPage(List.copyOf(items), nextCursor);
    }

    @Transactional
    public AiAgent add(String provider, String apiKey, Boolean active) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedApiKey = normalizeApiKey(apiKey);
        if (aiAgentRepository.existsByProviderIgnoreCaseAndApiKey(normalizedProvider, normalizedApiKey)) {
            throw new IllegalArgumentException("AI agent already exists for provider " + normalizedProvider);
        }

        AiAgent aiAgent = new AiAgent();
        aiAgent.setProvider(normalizedProvider);
        aiAgent.setApiKey(normalizedApiKey);
        aiAgent.setActive(active == null || active);
        AiAgent saved = aiAgentRepository.save(aiAgent);
        log.info("AI agent created: id={}, provider={}", saved.getId(), saved.getProvider());
        return saved;
    }

    @Transactional
    public AiAgent update(Long id, String provider, String apiKey, Boolean active) {
        AiAgent agent = aiAgentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AI agent not found: " + id));

        String nextProvider = provider == null ? agent.getProvider() : normalizeProvider(provider);
        String nextApiKey = apiKey == null ? agent.getApiKey() : normalizeApiKey(apiKey);
        boolean changedIdentity = !nextProvider.equalsIgnoreCase(agent.getProvider()) || !nextApiKey.equals(agent.getApiKey());
        if (changedIdentity && aiAgentRepository.existsByProviderIgnoreCaseAndApiKey(nextProvider, nextApiKey)) {
            throw new IllegalArgumentException("AI agent already exists for provider " + nextProvider);
        }

        agent.setProvider(nextProvider);
        agent.setApiKey(nextApiKey);
        if (active != null) agent.setActive(active);
        AiAgent saved = aiAgentRepository.save(agent);
        log.info("AI agent updated: id={}, provider={}", saved.getId(), saved.getProvider());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (!aiAgentRepository.existsById(id)) {
            throw new IllegalArgumentException("AI agent not found: " + id);
        }
        aiAgentRepository.deleteById(id);
        log.info("AI agent deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public EnhanceResult enhanceReviewText(String text, String mode) {
        String input = normalizeEnhanceText(text);
        String requestedMode = String.valueOf(mode == null ? "self_review" : mode).trim().toLowerCase();

        AiAgent agent = aiAgentRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .orElseThrow(() -> new IllegalArgumentException("No active AI agent configured."));
        String provider = normalizeProvider(agent.getProvider()).toLowerCase();
        if (!"openai".equals(provider)) {
            throw new IllegalArgumentException("AI provider is not wired yet: " + agent.getProvider());
        }

        String systemPrompt = buildSystemPrompt(requestedMode);
        String enhanced = callOpenAi(agent.getApiKey(), systemPrompt, input);
        return new EnhanceResult(enhanced, agent.getProvider(), defaultEnhanceModel);
    }

    @Transactional(readOnly = true)
    public Optional<ActiveAgentSummary> getActiveAgentSummary() {
        return aiAgentRepository.findFirstByActiveTrueOrderByUpdatedAtDesc()
                .map(agent -> new ActiveAgentSummary(agent.getProvider(), defaultEnhanceModel));
    }

    private int normalizeCursorLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_CURSOR_LIMIT;
        return Math.min(limit, MAX_CURSOR_LIMIT);
    }

    private Long parseCursorId(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            long id = Long.parseLong(cursor.trim());
            if (id <= 0) throw new IllegalArgumentException("Invalid cursor id.");
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid cursor id.");
        }
    }

    private String normalizeProvider(String provider) {
        String normalized = String.valueOf(provider == null ? "" : provider).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Provider is required");
        return normalized;
    }

    private String normalizeApiKey(String apiKey) {
        String normalized = String.valueOf(apiKey == null ? "" : apiKey).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("API key is required");
        return normalized;
    }

    private String normalizeEnhanceText(String text) {
        String normalized = String.valueOf(text == null ? "" : text).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Text is required for AI enhancement.");
        return normalized;
    }

    private String buildSystemPrompt(String mode) {
        if ("manager_review".equalsIgnoreCase(mode) || "manager".equalsIgnoreCase(mode)) {
            return "You are improving a manager review comment. Keep facts unchanged. Make it concise, clear, constructive, and professional. Use plain text only.";
        }
        return "You are improving a self review comment. Keep facts unchanged. Make it concise, clear, and professional. Use plain text only.";
    }

    private String callOpenAi(String apiKey, String systemPrompt, String userText) {
        try {
            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "model", defaultEnhanceModel,
                    "temperature", 0.3,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userText)
                    )
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalArgumentException("AI request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String enhanced = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (enhanced.isBlank()) throw new IllegalArgumentException("AI returned an empty response.");
            return enhanced;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to call AI provider.");
        }
    }

    public record CursorPage(List<AiAgent> items, String nextCursor) {}
    public record EnhanceResult(String text, String provider, String model) {}
    public record ActiveAgentSummary(String provider, String model) {}
}
