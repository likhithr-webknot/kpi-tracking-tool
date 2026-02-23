package com.webknot.kpi.service;

import com.webknot.kpi.models.BandDirectory;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.repository.BandDirectoryRepository;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Transactional(readOnly = true)
    public CursorPage list(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeLimit(limit);
        int offset = parseOffset(cursor);
        boolean active = Boolean.TRUE.equals(activeOnly);

        List<BandDirectory> sorted = bandDirectoryRepository.findAll().stream()
                .filter(row -> !active || row.isActive())
                .sorted(Comparator
                        .comparing(BandDirectory::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(row -> row.getCode() == null ? "" : row.getCode().name()))
                .toList();

        int safeOffset = Math.min(offset, sorted.size());
        int end = Math.min(safeOffset + pageSize, sorted.size());
        List<BandDirectory> items = sorted.subList(safeOffset, end);
        String nextCursor = end < sorted.size() ? String.valueOf(end) : null;
        return new CursorPage(List.copyOf(items), nextCursor);
    }

    @Transactional
    public BandDirectory add(String code, String label, Boolean active, Integer sortOrder) {
        CurrentBand parsedCode = parseBandCode(code);
        if (bandDirectoryRepository.existsById(parsedCode)) {
            throw new IllegalArgumentException("Band already exists: " + parsedCode.name());
        }
        BandDirectory row = new BandDirectory();
        row.setCode(parsedCode);
        row.setLabel(firstNonBlank(label, parsedCode.name()));
        row.setActive(active == null || active);
        row.setSortOrder(sortOrder != null ? sortOrder : parsedCode.ordinal());
        BandDirectory saved = bandDirectoryRepository.save(row);
        log.info("Band directory row created code={}", parsedCode.name());
        return saved;
    }

    @Transactional
    public BandDirectory update(String code, String label, Boolean active, Integer sortOrder) {
        CurrentBand parsedCode = parseBandCode(code);
        BandDirectory existing = bandDirectoryRepository.findById(parsedCode)
                .orElseThrow(() -> new IllegalArgumentException("Band not found: " + parsedCode.name()));

        if (label != null) {
            existing.setLabel(firstNonBlank(label, existing.getLabel(), parsedCode.name()));
        }
        if (active != null) {
            existing.setActive(active);
        }
        if (sortOrder != null) {
            existing.setSortOrder(sortOrder);
        }

        BandDirectory saved = bandDirectoryRepository.save(existing);
        log.info("Band directory row updated code={}", parsedCode.name());
        return saved;
    }

    @Transactional
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

