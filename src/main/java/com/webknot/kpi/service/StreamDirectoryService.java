package com.webknot.kpi.service;

import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.StreamDirectory;
import com.webknot.kpi.repository.StreamDirectoryRepository;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class StreamDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(StreamDirectoryService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final StreamDirectoryRepository streamDirectoryRepository;

    public StreamDirectoryService(StreamDirectoryRepository streamDirectoryRepository) {
        this.streamDirectoryRepository = streamDirectoryRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "stream-directory", unless = "#result == null || #result.items.isEmpty()")
    public CursorPage list(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeLimit(limit);
        int offset = parseOffset(cursor);
        boolean active = Boolean.TRUE.equals(activeOnly);

        List<StreamDirectory> sorted = streamDirectoryRepository.findAll().stream()
                .filter(row -> !active || row.isActive())
                .sorted(Comparator
                        .comparing(StreamDirectory::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(row -> row.getCode() == null ? "" : row.getCode().name()))
                .toList();

        int safeOffset = Math.min(offset, sorted.size());
        int end = Math.min(safeOffset + pageSize, sorted.size());
        List<StreamDirectory> items = sorted.subList(safeOffset, end);
        String nextCursor = end < sorted.size() ? String.valueOf(end) : null;
        return new CursorPage(List.copyOf(items), nextCursor);
    }

    @Transactional
    @CacheEvict(value = "stream-directory", allEntries = true)
    public StreamDirectory add(String code, String label, Boolean active, Integer sortOrder) {
        CurrentStream parsedCode = parseStreamCode(code);
        if (streamDirectoryRepository.existsById(parsedCode)) {
            throw new IllegalArgumentException("Stream already exists: " + parsedCode.name());
        }
        StreamDirectory row = new StreamDirectory();
        row.setCode(parsedCode);
        row.setLabel(firstNonBlank(label, parsedCode.name()));
        row.setActive(active == null || active);
        row.setSortOrder(sortOrder != null ? sortOrder : parsedCode.ordinal());
        StreamDirectory saved = streamDirectoryRepository.save(row);
        log.info("Stream directory row created code={}", parsedCode.name());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "stream-directory", allEntries = true)
    public StreamDirectory update(String code, String label, Boolean active, Integer sortOrder) {
        CurrentStream parsedCode = parseStreamCode(code);
        StreamDirectory existing = streamDirectoryRepository.findById(parsedCode)
                .orElseThrow(() -> new IllegalArgumentException("Stream not found: " + parsedCode.name()));

        if (label != null) {
            existing.setLabel(firstNonBlank(label, existing.getLabel(), parsedCode.name()));
        }
        if (active != null) {
            existing.setActive(active);
        }
        if (sortOrder != null) {
            existing.setSortOrder(sortOrder);
        }

        StreamDirectory saved = streamDirectoryRepository.save(existing);
        log.info("Stream directory row updated code={}", parsedCode.name());
        return saved;
    }

    @Transactional
    @CacheEvict(value = "stream-directory", allEntries = true)
    public void delete(String code) {
        CurrentStream parsedCode = parseStreamCode(code);
        if (!streamDirectoryRepository.existsById(parsedCode)) {
            throw new IllegalArgumentException("Stream not found: " + parsedCode.name());
        }
        streamDirectoryRepository.deleteById(parsedCode);
        log.info("Stream directory row deleted code={}", parsedCode.name());
    }

    private static CurrentStream parseStreamCode(String raw) {
        return BandStreamNormalizer.parseStream(raw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid stream code: " + raw));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value == null) continue;
            String trimmed = value.trim();
            if (!trimmed.isBlank()) return trimmed;
        }
        return null;
    }

    private static int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }

    private static int parseOffset(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        try {
            return Math.max(Integer.parseInt(raw.trim()), 0);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cursor. Must be a non-negative integer.");
        }
    }

    public record CursorPage(List<StreamDirectory> items, String nextCursor) {
    }
}

