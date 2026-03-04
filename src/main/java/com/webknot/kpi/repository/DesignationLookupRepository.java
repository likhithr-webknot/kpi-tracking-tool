package com.webknot.kpi.repository;

import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.DesignationLookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DesignationLookupRepository extends JpaRepository<DesignationLookup, DesignationLookup.DesignationId> {
    
    @Query("SELECT d FROM DesignationLookup d WHERE d.id.stream = :stream AND d.id.band = :band")
    Optional<DesignationLookup> findByStreamAndBand(@Param("stream") String stream, @Param("band") CurrentBand band);
    
    @Query("SELECT d FROM DesignationLookup d WHERE d.id.stream = :stream ORDER BY d.id.band")
    List<DesignationLookup> findByStream(@Param("stream") String stream);
    
    @Query("SELECT d FROM DesignationLookup d WHERE d.id.band = :band ORDER BY d.id.stream")
    List<DesignationLookup> findByBand(@Param("band") CurrentBand band);
}
