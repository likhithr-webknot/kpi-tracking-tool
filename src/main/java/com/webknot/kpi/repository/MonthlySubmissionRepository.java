package com.webknot.kpi.repository;

import com.webknot.kpi.models.MonthlySubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonthlySubmissionRepository extends JpaRepository<MonthlySubmission, Long> {

    Optional<MonthlySubmission> findByEmployee_EmployeeIdAndMonthAndSubmissionType(
            String employeeId,
            String month,
            String submissionType
    );

    List<MonthlySubmission> findByEmployee_EmployeeIdAndMonthOrderByUpdatedAtDesc(String employeeId, String month);

    List<MonthlySubmission> findByEmployee_EmployeeIdOrderByUpdatedAtDesc(String employeeId);

    List<MonthlySubmission> findByMonthOrderByUpdatedAtDesc(String month);

    List<MonthlySubmission> findByMonthAndStatusOrderByUpdatedAtDesc(String month, String status);

    List<MonthlySubmission> findByStatusOrderByUpdatedAtDesc(String status);

    List<MonthlySubmission> findAllByOrderByUpdatedAtDesc();
}
