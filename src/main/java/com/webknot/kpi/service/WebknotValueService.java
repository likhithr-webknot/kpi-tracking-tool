package com.webknot.kpi.service;

import com.webknot.kpi.models.WebknotValue;
import com.webknot.kpi.repository.WebknotValueRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WebknotValueService {
    private static final int DEFAULT_CURSOR_LIMIT = 10;
    private static final int MAX_CURSOR_LIMIT = 100;

    private final WebknotValueRepository webknotValueRepository;

    public WebknotValueService(WebknotValueRepository webknotValueRepository) {
        this.webknotValueRepository = webknotValueRepository;
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "webknot-values", unless = "#result == null || #result.items.isEmpty()")
    public CursorPage list(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeCursorLimit(limit);
        Long cursorId = parseCursorId(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        
        List<WebknotValue> results;
        if (activeOnly != null && activeOnly) {
            if (cursorId == null) {
                results = webknotValueRepository.findByActiveOrderByIdAsc(true, pageable);
            } else {
                results = webknotValueRepository.findByActiveAndIdGreaterThanOrderByIdAsc(true, cursorId, pageable);
            }
        } else {
            if (cursorId == null) {
                results = webknotValueRepository.findAllByOrderByIdAsc(pageable);
            } else {
                results = webknotValueRepository.findByIdGreaterThanOrderByIdAsc(cursorId, pageable);
            }
        }
        
        boolean hasMore = results.size() > pageSize;
        List<WebknotValue> items = hasMore ? results.subList(0, pageSize) : results;
        String nextCursor = hasMore && !items.isEmpty() ? String.valueOf(items.get(items.size() - 1).getId()) : null;
        
        return new CursorPage(items, nextCursor);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "webknot-values", allEntries = true)
    public WebknotValue add(String title, String pillar, String description, Boolean active) {
        String normalizedTitle = normalizeTitle(title);
        
        if (webknotValueRepository.existsByTitleIgnoreCase(normalizedTitle)) {
            throw new IllegalArgumentException("Webknot value with this title already exists: " + normalizedTitle);
        }
        
        WebknotValue value = new WebknotValue();
        value.setTitle(normalizedTitle);
        value.setPillar(clean(pillar));
        value.setDescription(clean(description));
        value.setActive(active != null ? active : true);
        
        return webknotValueRepository.save(value);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "webknot-values", allEntries = true)
    public WebknotValue update(Long id, String title, String pillar, String description, Boolean active) {
        WebknotValue value = webknotValueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webknot value not found: " + id));
        
        String normalizedTitle = normalizeTitle(title);
        
        // Check if title is being changed and if new title already exists
        if (!value.getTitle().equalsIgnoreCase(normalizedTitle)) {
            if (webknotValueRepository.existsByTitleIgnoreCase(normalizedTitle)) {
                throw new IllegalArgumentException("Webknot value with this title already exists: " + normalizedTitle);
            }
        }
        
        value.setTitle(normalizedTitle);
        value.setPillar(clean(pillar));
        value.setDescription(clean(description));
        if (active != null) {
            value.setActive(active);
        }
        
        return webknotValueRepository.save(value);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "webknot-values", allEntries = true)
    public void delete(Long id) {
        if (!webknotValueRepository.existsById(id)) {
            throw new IllegalArgumentException("Webknot value not found: " + id);
        }
        webknotValueRepository.deleteById(id);
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

    private String normalizeTitle(String title) {
        String normalized = clean(title);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Webknot value title is required");
        }
        return normalized;
    }

    private String clean(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    public record CursorPage(List<WebknotValue> items, String nextCursor) {}
}
