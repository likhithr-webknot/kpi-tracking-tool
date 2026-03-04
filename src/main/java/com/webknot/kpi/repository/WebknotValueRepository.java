package com.webknot.kpi.repository;

import com.webknot.kpi.models.WebknotValue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebknotValueRepository extends JpaRepository<WebknotValue, Long> {
    boolean existsByTitleIgnoreCase(String title);
    Optional<WebknotValue> findByTitleIgnoreCase(String title);

    List<WebknotValue> findAllByOrderByIdAsc(Pageable pageable);

    List<WebknotValue> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    List<WebknotValue> findByActiveOrderByIdAsc(boolean active, Pageable pageable);

    List<WebknotValue> findByActiveAndIdGreaterThanOrderByIdAsc(boolean active, Long id, Pageable pageable);
}
