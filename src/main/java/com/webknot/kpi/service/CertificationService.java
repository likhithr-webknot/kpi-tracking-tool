package com.webknot.kpi.service;

import com.webknot.kpi.models.Certification;
import com.webknot.kpi.repository.CertificationRepository;
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

    @Transactional(readOnly = true)
    public List<Certification> list(Boolean activeOnly) {
        return listCursor(activeOnly, null, null).items();
    }

    @Transactional(readOnly = true)
    public CursorPage listCursor(Boolean activeOnly, Integer limit, String cursor) {
        int pageSize = normalizeCursorLimit(limit);
        Long startAfter = parseCursorId(cursor);
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        boolean onlyActive = activeOnly == null || activeOnly;

        List<Certification> rows = onlyActive
                ? (startAfter == null
                    ? certificationRepository.findByActiveOrderByIdAsc(true, pageable)
                    : certificationRepository.findByActiveAndIdGreaterThanOrderByIdAsc(true, startAfter, pageable))
                : (startAfter == null
                    ? certificationRepository.findAllByOrderByIdAsc(pageable)
                    : certificationRepository.findByIdGreaterThanOrderByIdAsc(startAfter, pageable));

        boolean hasMore = rows.size() > pageSize;
        List<Certification> items = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasMore && !items.isEmpty()
                ? String.valueOf(items.get(items.size() - 1).getId())
                : null;

        return new CursorPage(List.copyOf(items), nextCursor);
    }

    @Transactional
    public Certification add(String name, Boolean active) {
        String normalizedName = normalizeName(name);
        if (certificationRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Certification already exists: " + normalizedName);
        }
        Certification certification = new Certification();
        certification.setName(normalizedName);
        certification.setActive(active == null || active);
        return certificationRepository.save(certification);
    }

    @Transactional
    public Certification update(Long id, String name, Boolean active) {
        Certification certification = certificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Certification not found: " + id));

        if (name != null) {
            String normalizedName = normalizeName(name);
            boolean nameChanged = !normalizedName.equalsIgnoreCase(certification.getName());
            if (nameChanged && certificationRepository.existsByNameIgnoreCase(normalizedName)) {
                throw new IllegalArgumentException("Certification already exists: " + normalizedName);
            }
            certification.setName(normalizedName);
        }

        if (active != null) certification.setActive(active);
        return certificationRepository.save(certification);
    }

    @Transactional
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
