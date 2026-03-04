package com.webknot.kpi.service;

import com.webknot.kpi.models.BandDirectory;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.repository.BandDirectoryRepository;
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
public class BandDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(BandDirectoryService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final BandDirectoryRepository bandDirectoryRepository;

    public BandDirectoryService(BandDirectoryRepository bandDirectoryRepository) {
        this.bandDirectoryRepository = bandDirectoryRepository;
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "band-directory", unless = "#result == null || #result.items.isEmpty()")
    public CursorPage list(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeLimit(limit);
        int offset = parseOffset(cursor);
        
        List<BandDirectory> all = activeOnly != null && activeOnly 
            ? bandDirectoryRepository.findAll().stream().filter(BandDirectory::isActive).toList()
            : bandDirectoryRepository.findAll();
        
        List<BandDirectory> sorted = all.stream()
            .sorted(Comparator.comparing(BandDirectory::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(bd -> bd.getCode().name()))
            .skip(offset)
            .limit(pageSize + 1)
            .toList();
        
        boolean hasMore = sorted.size() > pageSize;
        List<BandDirectory> items = hasMore ? sorted.subList(0, pageSize) : sorted;
        String nextCursor = hasMore ? String.valueOf(offset + pageSize) : null;
        
        return new CursorPage(items, nextCursor);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "band-directory", allEntries = true)
    public BandDirectory add(String code, String label, Boolean active, Integer sortOrder) {
        CurrentBand parsedCode = parseBandCode(code);
        
        if (bandDirectoryRepository.existsById(parsedCode)) {
            throw new IllegalArgumentException("Band already exists: " + parsedCode.name());
        }
        
        String finalLabel = firstNonBlank(label, parsedCode.name());
        if (finalLabel == null || finalLabel.isBlank()) {
            throw new IllegalArgumentException("Band label is required");
        }
        
        BandDirectory bandDirectory = new BandDirectory();
        bandDirectory.setCode(parsedCode);
        bandDirectory.setLabel(finalLabel);
        bandDirectory.setActive(active != null ? active : true);
        bandDirectory.setSortOrder(sortOrder);
        
        BandDirectory saved = bandDirectoryRepository.save(bandDirectory);
        log.info("Band directory row added code={} label={}", parsedCode.name(), finalLabel);
        return saved;
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "band-directory", allEntries = true)
    public BandDirectory update(String code, String label, Boolean active, Integer sortOrder) {
        CurrentBand parsedCode = parseBandCode(code);
        
        BandDirectory bandDirectory = bandDirectoryRepository.findById(parsedCode)
                .orElseThrow(() -> new IllegalArgumentException("Band not found: " + parsedCode.name()));
        
        String finalLabel = firstNonBlank(label, bandDirectory.getLabel());
        if (finalLabel == null || finalLabel.isBlank()) {
            throw new IllegalArgumentException("Band label is required");
        }
        
        bandDirectory.setLabel(finalLabel);
        if (active != null) {
            bandDirectory.setActive(active);
        }
        bandDirectory.setSortOrder(sortOrder);
        
        BandDirectory saved = bandDirectoryRepository.save(bandDirectory);
        log.info("Band directory row updated code={} label={}", parsedCode.name(), finalLabel);
        return saved;
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "band-directory", allEntries = true)
    public void delete(String code) {
        CurrentBand parsedCode = parseBandCode(code);
        if (!bandDirectoryRepository.existsById(parsedCode)) {
            throw new IllegalArgumentException("Band not found: " + parsedCode.name());
        }
        bandDirectoryRepository.deleteById(parsedCode);
        log.info("Band directory row deleted code={}", parsedCode.name());
    }

    private static CurrentBand parseBandCode(String raw) {
        return BandStreamNormalizer.parseBand(raw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid band code: " + raw));
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

    public record CursorPage(List<BandDirectory> items, String nextCursor) {
    }
}

