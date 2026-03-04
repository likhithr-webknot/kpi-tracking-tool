package com.webknot.kpi.service;

import com.webknot.kpi.models.Certification;
import com.webknot.kpi.repository.CertificationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CertificationService {
    private static final int DEFAULT_CURSOR_LIMIT = 10;
    private static final int MAX_CURSOR_LIMIT = 100;

    private final CertificationRepository certificationRepository;

    public CertificationService(CertificationRepository certificationRepository) {
        this.certificationRepository = certificationRepository;
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "certifications", unless = "#result == null || #result.isEmpty()")
    public List<Certification> list(Boolean activeOnly) {
        return listCursor(activeOnly, null, null).items();
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "certifications", unless = "#result == null || #result.items.isEmpty()")
    public CursorPage listCursor(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeCursorLimit(limit);
        Long cursorId = parseCursorId(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        
        List<Certification> results;
        if (activeOnly != null && activeOnly) {
            if (cursorId == null) {
                results = certificationRepository.findByActiveOrderByIdAsc(true, pageable);
            } else {
                results = certificationRepository.findByActiveAndIdGreaterThanOrderByIdAsc(true, cursorId, pageable);
            }
        } else {
            if (cursorId == null) {
                results = certificationRepository.findAllByOrderByIdAsc(pageable);
            } else {
                results = certificationRepository.findByIdGreaterThanOrderByIdAsc(cursorId, pageable);
            }
        }
        
        boolean hasMore = results.size() > pageSize;
        List<Certification> items = hasMore ? results.subList(0, pageSize) : results;
        String nextCursor = hasMore && !items.isEmpty() ? String.valueOf(items.get(items.size() - 1).getId()) : null;
        
        return new CursorPage(items, nextCursor);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "certifications", allEntries = true)
    public Certification add(String name, Boolean active) {
        String normalizedName = normalizeName(name);
        
        if (certificationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Certification with this name already exists: " + normalizedName);
        }
        
        Certification certification = new Certification();
        certification.setName(normalizedName);
        certification.setActive(active != null ? active : true);
        
        return certificationRepository.save(certification);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "certifications", allEntries = true)
    public Certification update(Long id, String name, Boolean active) {
        Certification certification = certificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certification not found: " + id));
        
        String normalizedName = normalizeName(name);
        
        // Check if name is being changed and if new name already exists
        if (!certification.getName().equalsIgnoreCase(normalizedName)) {
            if (certificationRepository.existsByNameIgnoreCase(normalizedName)) {
                throw new IllegalArgumentException("Certification with this name already exists: " + normalizedName);
            }
        }
        
        certification.setName(normalizedName);
        if (active != null) {
            certification.setActive(active);
        }
        
        return certificationRepository.save(certification);
    }

    @Transactional(timeout = 10)
    @CacheEvict(value = "certifications", allEntries = true)
    public void delete(Long id) {
        if (!certificationRepository.existsById(id)) {
            throw new IllegalArgumentException("Certification not found: " + id);
        }
        certificationRepository.deleteById(id);
    }

    private String normalizeName(String name) {
        String normalized = String.valueOf(name == null ? "" : name).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("Certification name is required");
        return normalized;
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

    public record CursorPage(List<Certification> items, String nextCursor) {}
}
