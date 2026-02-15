package com.webknot.kpi.repository;

import com.webknot.kpi.models.SubmissionCycle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionCycleRepository extends JpaRepository<SubmissionCycle, UUID> {
    Optional<SubmissionCycle> findByCycleKey(String cycleKey);
}
