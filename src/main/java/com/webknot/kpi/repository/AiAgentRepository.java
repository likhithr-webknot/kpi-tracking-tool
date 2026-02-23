package com.webknot.kpi.repository;

import com.webknot.kpi.models.AiAgent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiAgentRepository extends JpaRepository<AiAgent, Long> {

    boolean existsByProviderIgnoreCaseAndApiKey(String provider, String apiKey);

    List<AiAgent> findAllByOrderByIdAsc(Pageable pageable);

    List<AiAgent> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

    List<AiAgent> findByActiveOrderByIdAsc(boolean active, Pageable pageable);

    List<AiAgent> findByActiveAndIdGreaterThanOrderByIdAsc(boolean active, Long id, Pageable pageable);

    Optional<AiAgent> findFirstByActiveTrueOrderByUpdatedAtDesc();
}
