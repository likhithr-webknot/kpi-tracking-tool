package com.webknot.kpi.repository;

import com.webknot.kpi.models.BandDirectory;
import com.webknot.kpi.models.CurrentBand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BandDirectoryRepository extends JpaRepository<BandDirectory, CurrentBand> {
}

