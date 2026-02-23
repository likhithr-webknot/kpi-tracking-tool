package com.webknot.kpi.repository;

import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Employee> findByEmpRole(EmployeeRole role);

    List<Employee> findByManager_EmployeeId(String managerId);

    List<Employee> findAllByOrderByEmployeeIdAsc(Pageable pageable);

    List<Employee> findByEmployeeIdGreaterThanOrderByEmployeeIdAsc(String employeeId, Pageable pageable);

    long countByEmpRole(EmployeeRole role);

    @Query("select count(distinct e.band) from Employee e")
    long countDistinctBand();
}
