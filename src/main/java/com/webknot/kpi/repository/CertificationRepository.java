package com.webknot.kpi.repository;

import com.webknot.kpi.models.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    boolean existsByNameIgnoreCase(String name);

    List<Certification> findByActiveOrderByNameAsc(boolean active);

    List<Certification> findAllByOrderByNameAsc();

    List<Certification> findByActiveOrderByIdAsc(boolean active, Pageable pageable);

    List<Certification> findByActiveAndIdGreaterThanOrderByIdAsc(boolean active, Long id, Pageable pageable);

    List<Certification> findAllByOrderByIdAsc(Pageable pageable);

    List<Certification> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}
