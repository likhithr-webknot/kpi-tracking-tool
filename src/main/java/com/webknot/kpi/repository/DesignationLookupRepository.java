package com.webknot.kpi.repository;

import com.webknot.kpi.models.DesignationLookup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignationLookupRepository extends JpaRepository<DesignationLookup, DesignationLookup.DesignationId> {
}
