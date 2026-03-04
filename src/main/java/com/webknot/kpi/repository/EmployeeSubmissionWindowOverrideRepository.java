package com.webknot.kpi.repository;

import com.webknot.kpi.models.EmployeeSubmissionWindowOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeSubmissionWindowOverrideRepository extends JpaRepository<EmployeeSubmissionWindowOverride, String> {
    Optional<EmployeeSubmissionWindowOverride> findByEmployeeId(String employeeId);
}
