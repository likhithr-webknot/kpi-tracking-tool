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

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "stream-directory", unless = "#result == null || #result.items.isEmpty()")
    public CursorPage list(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeLimit(limit);
        int offset = parseOffset(cursor);
        
        List<StreamDirectory> all = activeOnly != null && activeOnly 
            ? streamDirectoryRepository.findAll().stream().filter(StreamDirectory::isActive).toList()
            : streamDirectoryRepository.findAll();
        
        List<StreamDirectory> sorted = all.stream()
            .sorted(Comparator.comparing(StreamDirectory::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(sd -> sd.getCode().name()))
            .skip(offset)
            .limit(pageSize + 1)
            .toList();
        
        boolean hasMore = sorted.size() > pageSize;
        List<StreamDirectory> items = hasMore ? sorted.subList(0, pageSize) : sorted;
        String nextCursor = hasMore ? String.valueOf(offset + pageSize) : null;
        
        return new CursorPage(items, nextCursor);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "stream-directory", allEntries = true)
    public StreamDirectory add(String code, String label, Boolean active, Integer sortOrder) {
        CurrentStream parsedCode = parseStreamCode(code);
        
        if (streamDirectoryRepository.existsById(parsedCode)) {
            throw new IllegalArgumentException("Stream already exists: " + parsedCode.name());
        }
        
        String finalLabel = firstNonBlank(label, parsedCode.name());
        if (finalLabel == null || finalLabel.isBlank()) {
            throw new IllegalArgumentException("Stream label is required");
        }
        
        StreamDirectory streamDirectory = new StreamDirectory();
        streamDirectory.setCode(parsedCode);
        streamDirectory.setLabel(finalLabel);
        streamDirectory.setActive(active != null ? active : true);
        streamDirectory.setSortOrder(sortOrder);
        
        StreamDirectory saved = streamDirectoryRepository.save(streamDirectory);
        log.info("Stream directory row added code={} label={}", parsedCode.name(), finalLabel);
        return saved;
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "stream-directory", allEntries = true)
    public StreamDirectory update(String code, String label, Boolean active, Integer sortOrder) {
        CurrentStream parsedCode = parseStreamCode(code);
        
        StreamDirectory streamDirectory = streamDirectoryRepository.findById(parsedCode)
                .orElseThrow(() -> new IllegalArgumentException("Stream not found: " + parsedCode.name()));
        
        String finalLabel = firstNonBlank(label, streamDirectory.getLabel());
        if (finalLabel == null || finalLabel.isBlank()) {
            throw new IllegalArgumentException("Stream label is required");
        }
        
        streamDirectory.setLabel(finalLabel);
        if (active != null) {
            streamDirectory.setActive(active);
        }
        streamDirectory.setSortOrder(sortOrder);
        
        StreamDirectory saved = streamDirectoryRepository.save(streamDirectory);
        log.info("Stream directory row updated code={} label={}", parsedCode.name(), finalLabel);
        return saved;
    }

    @Transactional(timeout = 10)
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

